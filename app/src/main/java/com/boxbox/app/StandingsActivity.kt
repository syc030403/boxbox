package com.boxbox.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.boxbox.app.cache.CachedApi
import com.boxbox.app.databinding.ActivityStandingsBinding
import com.boxbox.app.databinding.ItemStandingBinding
import com.boxbox.app.model.ConstructorStanding
import com.boxbox.app.model.ConstructorStandingsApiResponse
import com.boxbox.app.model.DriverStanding
import com.boxbox.app.model.DriverStandingsApiResponse
import com.boxbox.app.network.RetrofitClient
import com.boxbox.app.util.DateTimeUtils
import com.boxbox.app.util.TeamInfo
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class StandingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStandingsBinding
    private var season = MainActivity.AVAILABLE_SEASONS.first()

    private var driverStandings: List<DriverStanding>? = null
    private var constructorStandings: List<ConstructorStanding>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStandingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        season = intent.getIntExtra(MainActivity.EXTRA_SEASON, season)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.standings_title, season)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = renderCurrentTab()
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        loadStandings()
    }

    private fun loadStandings() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
        binding.standingsListLayout.removeAllViews()

        lifecycleScope.launch {
            try {
                val driversResult = CachedApi.fetch(
                    context = this@StandingsActivity,
                    key = "driverstandings_$season",
                    clazz = DriverStandingsApiResponse::class.java
                ) { RetrofitClient.api.getDriverStandings(season) }

                val constructorsResult = CachedApi.fetch(
                    context = this@StandingsActivity,
                    key = "constructorstandings_$season",
                    clazz = ConstructorStandingsApiResponse::class.java
                ) { RetrofitClient.api.getConstructorStandings(season) }

                driverStandings = driversResult.data.mrData.standingsTable.standingsLists
                    .firstOrNull()?.driverStandings.orEmpty()
                constructorStandings = constructorsResult.data.mrData.standingsTable.standingsLists
                    .firstOrNull()?.constructorStandings.orEmpty()

                val isStale = driversResult.isStale || constructorsResult.isStale
                val savedAt = minOf(
                    driversResult.savedAtMillis ?: Long.MAX_VALUE,
                    constructorsResult.savedAtMillis ?: Long.MAX_VALUE
                )
                updateOfflineBanner(isStale, savedAt.takeIf { it != Long.MAX_VALUE })

                renderCurrentTab()
            } catch (e: Exception) {
                Log.e("StandingsActivity", "Failed to load standings", e)
                binding.errorText.visibility = View.VISIBLE
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
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

    private fun renderCurrentTab() {
        binding.standingsListLayout.removeAllViews()
        when (binding.tabLayout.selectedTabPosition) {
            0 -> driverStandings?.forEach { addDriverRow(it) }
            1 -> constructorStandings?.forEach { addConstructorRow(it) }
        }
    }

    private fun addDriverRow(standing: DriverStanding) {
        val rowBinding = ItemStandingBinding.inflate(
            LayoutInflater.from(this), binding.standingsListLayout, false
        )
        rowBinding.positionText.text = standing.position
        rowBinding.nameText.text = "${standing.driver.givenName} ${standing.driver.familyName}"
        val constructor = standing.constructors.firstOrNull()
        rowBinding.subtitleText.text = constructor?.name.orEmpty()
        TeamInfo.applyChip(rowBinding.teamChip, constructor?.constructorId)
        TeamInfo.applyAccentBar(rowBinding.accentBar, constructor?.constructorId)
        rowBinding.pointsText.text = "${standing.points} PTS"
        rowBinding.winsText.text = "${standing.wins}승"
        binding.standingsListLayout.addView(rowBinding.root)
    }

    private fun addConstructorRow(standing: ConstructorStanding) {
        val rowBinding = ItemStandingBinding.inflate(
            LayoutInflater.from(this), binding.standingsListLayout, false
        )
        rowBinding.positionText.text = standing.position
        rowBinding.nameText.text = standing.constructor.name
        rowBinding.subtitleText.text = standing.constructor.nationality.orEmpty()
        TeamInfo.applyChip(rowBinding.teamChip, standing.constructor.constructorId)
        TeamInfo.applyAccentBar(rowBinding.accentBar, standing.constructor.constructorId)
        rowBinding.pointsText.text = "${standing.points} PTS"
        rowBinding.winsText.text = "${standing.wins}승"
        binding.standingsListLayout.addView(rowBinding.root)
    }
}
