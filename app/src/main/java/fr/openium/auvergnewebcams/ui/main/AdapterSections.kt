package fr.openium.auvergnewebcams.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.leochuan.ScaleLayoutManager
import fr.openium.auvergnewebcams.R
import fr.openium.auvergnewebcams.custom.SnapOnScrollListener
import fr.openium.auvergnewebcams.ext.attachSnapHelperWithListener
import fr.openium.auvergnewebcams.model.entity.Section
import fr.openium.auvergnewebcams.model.entity.Webcam
import fr.openium.auvergnewebcams.utils.ImageUtils
import fr.openium.kotlintools.ext.dip
import kotlinx.android.synthetic.main.item_section.view.*
import java.util.*


/**
 * Created by Openium on 19/02/2019.
 */
class AdapterSections(
    private val context: Context,
    private var sections: List<Section>,
    private val onSectionClicked: ((Section) -> Unit),
    private val onWebcamClicked: ((Webcam) -> Unit)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewPool = RecyclerView.RecycledViewPool()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return SectionHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_section, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Get item in list
        val item = sections[position]

        // --- Set Header information

        // Set section title
        holder.itemView.textViewSectionName.text = item.title

        // Image name have "-" instead of "_", we need to do the change
        val imageResourceID = ImageUtils.getImageResourceAssociatedToSection(context, item)

        // Set the right section icon
        Glide.with(context)
            .load(imageResourceID)
            .override(dip(context, 50f).toInt(), dip(context, 50f).toInt())
            .into(holder.itemView.imageViewSection)

        // Set the number of camera to show in the subtitle
        val nbWebCams = String.format(
            Locale.getDefault(),
            context.resources.getQuantityString(R.plurals.nb_cameras_format, item.webcams.count(), item.webcams.count())
        )
        holder.itemView.textViewSectionNbCameras.text = nbWebCams

        // Set on click on Section part
        holder.itemView.linearLayoutSection.setOnClickListener {
            onSectionClicked.invoke(item)
        }

        // --- Set Item information

        // Create and set adapter only if this is not already done
        if (holder.itemView.recyclerViewWebcams.adapter == null) {
            val adapterSectionWebcams = AdapterSectionWebcams(context, item.webcams, onWebcamClicked)

            // Applying all settings to the RecyclerView
            holder.itemView.recyclerViewWebcams.apply {
                adapter = adapterSectionWebcams
                layoutManager = ScaleLayoutManager(context, dip(-10f).toInt(), ScaleLayoutManager.HORIZONTAL).apply {
                    minAlpha = 0.5f
                    maxAlpha = 1f
                    maxVisibleItemCount = 3
                    infinite = true
                }

                // Some optimization
                setHasFixedSize(true)

                setRecycledViewPool(viewPool)
            }
        } else {
            holder.itemView.recyclerViewWebcams.also {
                // Remove all previous listeners
                it.clearOnScrollListeners()
                it.onFlingListener = null

                // Update webcams list
                (it.adapter as AdapterSectionWebcams).refreshData(item.webcams)
            }
        }

        // This is needed to scroll 1 by 1, and get notified about position changing
        holder.itemView.recyclerViewWebcams.attachSnapHelperWithListener(
            PagerSnapHelper(),
            SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL,
            object : SnapOnScrollListener.OnSnapPositionChangeListener {
                override fun onSnapPositionChange(position: Int) {
                    holder.itemView.textViewWebcamName.text = item.webcams[position].title
                }
            }
        )
    }

    override fun getItemCount(): Int {
        return sections.size
    }

    fun refreshData(sectionList: List<Section>) {
        sections = sectionList
        notifyDataSetChanged()
    }

    class SectionHolder(view: View) : RecyclerView.ViewHolder(view)
}