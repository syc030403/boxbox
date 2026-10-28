package com.boxbox.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.boxbox.app.cache.CachedApi
import com.boxbox.app.databinding.ActivityMainBinding
import com.boxbox.app.model.RaceApiResponse
import com.boxbox.app.network.RetrofitClient
import com.boxbox.app.notification.NotificationHelper
import com.boxbox.app.util.DateTimeUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        val AVAILABLE_SEASONS = listOf(2026, 2025, 2024)
        const val EXTRA_SEASON = "extra_season"
    }

    private lateinit var binding: ActivityMainBinding
    private var season = AVAILABLE_SEASONS.first()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        updateTitle()
        NotificationHelper.createChannel(this)

        binding.raceRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.swipeRefresh.setOnRefreshListener { loadRaces(forceRefresh = true) }
        loadRaces()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val seasonButton = menu.findItem(R.id.action_season).actionView as TextView
        seasonButton.text = "$season ▾"
        seasonButton.setOnClickListener { showSeasonPicker(seasonButton) }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_standings -> {
                startActivity(
                    Intent(this, StandingsActivity::class.java).putExtra(EXTRA_SEASON, season)
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSeasonPicker(anchor: View) {
        val popup = PopupMenu(this, anchor)
        AVAILABLE_SEASONS.forEach { popup.menu.add(Menu.NONE, it, Menu.NONE, "$it 시즌") }
        popup.setOnMenuItemClickListener { item ->
            if (season != item.itemId) {
                season = item.itemId
                updateTitle()
                invalidateOptionsMenu()
                loadRaces()
            }
            true
        }
        popup.show()
    }

    private fun updateTitle() {
        supportActionBar?.title = getString(R.string.season_title, season)
    }

    private fun loadRaces(forceRefresh: Boolean = false) {
        if (!forceRefresh) showLoading()
        lifecycleScope.launch {
            try {
                val result = CachedApi.fetch(
                    context = this@MainActivity,
                    key = "races_$season",
                    clazz = RaceApiResponse::class.java,
                    forceRefresh = forceRefresh
                ) { RetrofitClient.api.getRaces(season) }

                val races = result.data.mrData.raceTable.races.sortedBy { it.round.toIntOrNull() ?: 0 }
                binding.raceRecyclerView.adapter = RaceAdapter(races)
                updateOfflineBanner(result.isStale, result.savedAtMillis)
                showContent()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to load races", e)
                showError()
            } finally {
                binding.swipeRefresh.isRefreshing = false
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

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
        binding.raceRecyclerView.visibility = View.GONE
    }

    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.errorText.visibility = View.GONE
        binding.raceRecyclerView.visibility = View.VISIBLE
    }

    private fun showError() {
        binding.progressBar.visibility = View.GONE
        binding.errorText.visibility = View.VISIBLE
        binding.raceRecyclerView.visibility = View.GONE
    }
}
