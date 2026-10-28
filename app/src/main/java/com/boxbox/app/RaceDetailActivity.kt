package com.boxbox.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.boxbox.app.cache.CachedApi
import com.boxbox.app.databinding.ActivityRaceDetailBinding
import com.boxbox.app.databinding.ItemResultBinding
import com.boxbox.app.databinding.ItemSessionBinding
import com.boxbox.app.model.DriverResult
import com.boxbox.app.model.Race
import com.boxbox.app.model.ResultsApiResponse
import com.boxbox.app.model.Session
import com.boxbox.app.network.RetrofitClient
import com.boxbox.app.notification.OffsetFormat
import com.boxbox.app.notification.ReminderEntry
import com.boxbox.app.notification.ReminderScheduler
import com.boxbox.app.notification.ReminderStore
import com.boxbox.app.notification.SessionLabels
import com.boxbox.app.util.DateTimeUtils
import com.boxbox.app.util.TeamInfo
import kotlinx.coroutines.launch
import java.time.LocalDate

class RaceDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RACE = "extra_race"
        private const val RESULTS_TTL_MILLIS = 24 * 60 * 60 * 1000L
    }

    private lateinit var binding: ActivityRaceDetailBinding
    private var race: Race? = null
    private var isRaceDone = false
    private var pendingReminderRequest: (() -> Unit)? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingReminderRequest
        pendingReminderRequest = null
        if (granted) {
            request?.invoke()
        } else {
            Toast.makeText(this, R.string.reminder_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val race = getRaceExtra() ?: run {
            finish()
            return
        }
        this.race = race

        bindRace(race)
    }

    private fun getRaceExtra(): Race? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RACE, Race::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RACE)
        }

    private fun bindRace(race: Race) {
        supportActionBar?.title = race.raceName
        binding.roundText.text = "ROUND ${race.round.toIntOrNull()?.let { "%02d".format(it) } ?: race.round}"
        binding.raceNameText.text = race.raceName
        binding.circuitNameText.text = race.circuit.circuitName

        val location = race.circuit.location
        binding.locationText.text = listOfNotNull(location?.locality, location?.country)
            .joinToString(", ")

        isRaceDone = DateTimeUtils.isPast(race.date, race.time)
        val statusColor = if (isRaceDone) "#4CAF50" else "#E10600"
        binding.statusText.text = getString(if (isRaceDone) R.string.status_done else R.string.status_upcoming)
        binding.statusText.setTextColor(Color.WHITE)
        (binding.statusText.background.mutate() as GradientDrawable).setColor(Color.parseColor(statusColor))

        binding.sessionListLayout.removeAllViews()
        val season = LocalDate.parse(race.date).year
        val sessions = listOfNotNull(
            race.firstPractice?.let { Triple(R.string.session_fp1, "fp1", it) },
            race.secondPractice?.let { Triple(R.string.session_fp2, "fp2", it) },
            race.thirdPractice?.let { Triple(R.string.session_fp3, "fp3", it) },
            race.sprintQualifying?.let { Triple(R.string.session_sprint_qualifying, "sq", it) },
            race.sprint?.let { Triple(R.string.session_sprint, "sprint", it) },
            race.qualifying?.let { Triple(R.string.session_qualifying, "quali", it) }
        )
        sessions.forEach { (nameRes, sessionType, session) ->
            addSessionRow(race, season, nameRes, sessionType, session)
        }
        addSessionRow(race, season, R.string.session_race, "race", Session(race.date, race.time))

        if (isRaceDone) {
            binding.resultsSection.visibility = View.VISIBLE
            loadResults(race)
        } else {
            binding.resultsSection.visibility = View.GONE
        }
    }

    private fun addSessionRow(race: Race, season: Int, nameRes: Int, sessionType: String, session: Session) {
        val rowBinding = ItemSessionBinding.inflate(
            LayoutInflater.from(this), binding.sessionListLayout, false
        )
        rowBinding.sessionNameText.text = getString(nameRes)
        rowBinding.sessionTimeText.text = DateTimeUtils.formatKst(session.date, session.time)

        if (isRaceDone) {
            rowBinding.sessionReminderButton.visibility = View.GONE
        } else {
            rowBinding.sessionReminderButton.visibility = View.VISIBLE
            updateReminderIcon(rowBinding, season, race.round, sessionType)
            rowBinding.sessionReminderButton.setOnClickListener {
                onReminderButtonClicked(race, season, sessionType, session, rowBinding)
            }
        }

        binding.sessionListLayout.addView(rowBinding.root)
    }

    private fun updateReminderIcon(rowBinding: ItemSessionBinding, season: Int, round: String, sessionType: String) {
        val active = ReminderStore.hasAnyFor(this, season, round, sessionType)
        rowBinding.sessionReminderButton.imageTintList =
            android.content.res.ColorStateList.valueOf(
                Color.parseColor(if (active) "#E10600" else "#808088")
            )
    }

    private fun onReminderButtonClicked(
        race: Race, season: Int, sessionType: String, session: Session, rowBinding: ItemSessionBinding
    ) {
        val action = { showReminderDialog(race, season, sessionType, session, rowBinding) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminderRequest = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        action()
    }

    private fun showReminderDialog(
        race: Race, season: Int, sessionType: String, session: Session, rowBinding: ItemSessionBinding
    ) {
        val sessionStartMillis = DateTimeUtils.toEpochMillis(session.date, session.time)
        val enabled = ReminderStore.enabledOffsetsFor(this, season, race.round, sessionType)

        val labels = OffsetFormat.PRESET_MINUTES.map {
            getString(R.string.offset_minutes_before, OffsetFormat.label(this, it))
        }
        val checked = OffsetFormat.PRESET_MINUTES.map { it in enabled }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.reminder_dialog_title_session, SessionLabels.label(this, sessionType)))
            .setMultiChoiceItems(labels.toTypedArray(), checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                applyReminderSelection(race, season, sessionType, sessionStartMillis, checked)
                updateReminderIcon(rowBinding, season, race.round, sessionType)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyReminderSelection(
        race: Race, season: Int, sessionType: String, sessionStartMillis: Long, checked: BooleanArray
    ) {
        if (checked.any { it } && !ReminderScheduler.canScheduleExactAlarms(this)) {
            Toast.makeText(this, R.string.reminder_exact_alarm_denied, Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                )
            }
            return
        }

        var scheduledCount = 0
        OffsetFormat.PRESET_MINUTES.forEachIndexed { index, offsetMinutes ->
            val key = ReminderEntry.makeKey(season, race.round, sessionType, offsetMinutes)
            val triggerAt = sessionStartMillis - offsetMinutes * 60_000L
            if (checked[index]) {
                if (triggerAt > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(
                        this,
                        ReminderEntry(key, season, race.round, race.raceName, sessionType, offsetMinutes, triggerAt)
                    )
                    scheduledCount++
                }
            } else {
                ReminderScheduler.cancel(this, key)
            }
        }
        Toast.makeText(this, getString(R.string.reminder_set_toast, scheduledCount), Toast.LENGTH_SHORT).show()
    }

    private fun loadResults(race: Race) {
        val season = LocalDate.parse(race.date).year
        val round = race.round.toIntOrNull() ?: return

        binding.resultsProgressBar.visibility = View.VISIBLE
        binding.resultsErrorText.visibility = View.GONE
        binding.resultsListLayout.removeAllViews()

        lifecycleScope.launch {
            try {
                val result = CachedApi.fetch(
                    context = this@RaceDetailActivity,
                    key = "results_${season}_$round",
                    clazz = ResultsApiResponse::class.java,
                    ttlMillis = RESULTS_TTL_MILLIS
                ) { RetrofitClient.api.getRaceResults(season, round) }

                val results = result.data.mrData.raceTable.races.firstOrNull()?.results.orEmpty()
                if (results.isEmpty()) {
                    showResultsError()
                } else {
                    results.forEach { addResultRow(it) }
                    updateOfflineBanner(result.isStale, result.savedAtMillis)
                }
            } catch (e: Exception) {
                Log.e("RaceDetailActivity", "Failed to load results", e)
                showResultsError()
            } finally {
                binding.resultsProgressBar.visibility = View.GONE
            }
        }
    }

    private fun showResultsError() {
        binding.resultsErrorText.visibility = View.VISIBLE
    }

    private fun updateOfflineBanner(isStale: Boolean, savedAtMillis: Long?) {
        if (isStale && savedAtMillis != null) {
            binding.offlineBanner.text = getString(
                R.string.offline_banner, DateTimeUtils.formatLocalTimestamp(savedAtMillis)
            )
            binding.offlineBanner.visibility = View.VISIBLE
        } else {
            binding.offlineBanner.visibility = View.GONE
        }
    }

    private fun addResultRow(result: DriverResult) {
        val rowBinding = ItemResultBinding.inflate(
            LayoutInflater.from(this), binding.resultsListLayout, false
        )
        rowBinding.positionText.text = result.position
        rowBinding.driverNameText.text = "${result.driver.givenName} ${result.driver.familyName}"
        rowBinding.constructorNameText.text = result.constructor.name
        TeamInfo.applyChip(rowBinding.teamChip, result.constructor.constructorId)
        TeamInfo.applyAccentBar(rowBinding.accentBar, result.constructor.constructorId)
        rowBinding.resultStatusText.text = result.time?.time ?: result.status
        rowBinding.pointsText.text = "${result.points} PTS"
        binding.resultsListLayout.addView(rowBinding.root)
    }
}
