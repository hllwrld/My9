package peakchao.com.porn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import peakchao.com.porn.R
import peakchao.com.porn.model.PornModel

class VideoListAdapter(
    private val onClick: (PornModel) -> Unit
) : RecyclerView.Adapter<VideoListAdapter.VH>() {

    private val items = mutableListOf<PornModel>()

    fun setData(list: List<PornModel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun addData(list: List<PornModel>) {
        val start = items.size
        items.addAll(list)
        notifyItemRangeInserted(start, list.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title ?: ""
        holder.tvDuration.text = item.duration ?: ""
        holder.tvInfo.text = item.info ?: ""
        Glide.with(holder.ivImg.context)
            .load(item.imgUrl)
            .centerCrop()
            .into(holder.ivImg)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivImg: ImageView = view.findViewById(R.id.iv_img)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvDuration: TextView = view.findViewById(R.id.tv_duration)
        val tvInfo: TextView = view.findViewById(R.id.tv_info)
    }
}
