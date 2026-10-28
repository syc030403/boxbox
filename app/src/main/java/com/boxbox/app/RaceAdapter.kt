package com.boxbox.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boxbox.app.databinding.ItemRaceBinding
import com.boxbox.app.model.Race
import com.boxbox.app.util.DateTimeUtils

class RaceAdapter(private val races: List<Race>) :
    RecyclerView.Adapter<RaceAdapter.RaceViewHolder>() {

    inner class RaceViewHolder(val binding: ItemRaceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaceViewHolder {
        val binding = ItemRaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RaceViewHolder, position: Int) {
        val race = races[position]
        val binding = holder.binding

        val roundNumber = race.round.toIntOrNull()?.let { "%02d".format(it) } ?: race.round
        binding.roundText.text = "ROUND $roundNumber"
        binding.raceNameText.text = race.raceName
        binding.circuitNameText.text = race.circuit.circuitName
        binding.dateText.text = DateTimeUtils.formatKst(race.date, race.time)

        val isDone = DateTimeUtils.isPast(race.date, race.time)
        val statusColor = if (isDone) "#4CAF50" else "#E10600"
        binding.statusText.text = binding.root.context.getString(
            if (isDone) R.string.status_done else R.string.status_upcoming
        )
        (binding.statusText.background.mutate() as GradientDrawable).setColor(Color.parseColor(statusColor))
        binding.accentBar.setBackgroundColor(Color.parseColor(statusColor))

        binding.root.setOnClickListener {
            val context = binding.root.context
            val intent = Intent(context, RaceDetailActivity::class.java).apply {
                putExtra(RaceDetailActivity.EXTRA_RACE, race)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = races.size
}
