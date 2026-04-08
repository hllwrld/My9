package peakchao.com.porn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import peakchao.com.porn.R
import peakchao.com.porn.model.PornEvaluator

class EvaluatorAdapter : RecyclerView.Adapter<EvaluatorAdapter.VH>() {

    private val items = mutableListOf<PornEvaluator>()

    fun setData(list: List<PornEvaluator>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evaluator, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.evaluatorName ?: ""
        holder.tvTime.text = item.evaluatorTime ?: ""
        holder.tvContent.text = item.evaluatorContent ?: ""
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvContent: TextView = view.findViewById(R.id.tv_content)
    }
}
