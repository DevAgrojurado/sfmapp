package com.agrojurado.sfmappv2.presentation.ui.home.fragmenthome

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agrojurado.sfmappv2.databinding.ListHomeBinding

class HomeAdapter(private val onItemClick: (HomeViewModel.HomeItem) -> Unit) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    private var items: List<HomeViewModel.HomeItem> = emptyList()

    inner class ViewHolder(private val binding: ListHomeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeViewModel.HomeItem) {
            binding.tittleTv.text = item.title
            binding.descriptionTv.text = item.description
            binding.ivEvaluacion.setImageResource(item.imageResId)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<HomeViewModel.HomeItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}