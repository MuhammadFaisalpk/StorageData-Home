package com.example.storage_data.adapter

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.storage_data.R
import com.example.storage_data.model.MyModel
import com.example.storage_data.model.SelectedModel
import com.example.storage_data.activities.VideoPlayerActivity
import com.example.storage_data.utils.MySingelton
import com.example.storage_data.utils.ViewTypeInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File


class VideosListAdapter(private val context: Fragment) :
    RecyclerView.Adapter<VideosListAdapter.ViewHolder>() {

    var items: ArrayList<MyModel>? = null
    private var checkList: ArrayList<SelectedModel>? = ArrayList()
    var newItem: MyModel? = null
    private var newPosition = 0
    private val listItem = 0
    private val gridItem = 1
    var isSwitchView = true

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView: View = if (viewType == listItem) {
            LayoutInflater.from(parent.context).inflate(R.layout.images_list_design, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.images_grid_design, parent, false)
        }
        return ViewHolder(itemView)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val videos = items?.get(position)

            holder.nameHolder.text = items?.get(position)?.title
            holder.fnameHolder.text = items?.get(position)?.folderName

            val imagePath: String? = items?.get(position)?.path

            Glide.with(context)
                .load(imagePath)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageHolder)

            if (checkList?.get(position)?.selected == true) {
                holder.clMain.setBackgroundResource(R.color.purple_200)
            } else {
                holder.clMain.setBackgroundResource(android.R.color.transparent)
            }

            holder.itemView.setOnClickListener() {
                val intent = Intent(it.context, VideoPlayerActivity::class.java)
                intent.putExtra("video_data", videos)
                it.context.startActivity(intent)
            }
            holder.itemView.setOnLongClickListener {
                val check = checkList?.get(position)?.selected
                val value = checkList?.get(position)?.item

                if (check == true) {
                    if (value != null) {
                        MySingelton.removeSelectedVideos(value)
                    }

                    checkList?.removeAt(position)
                    checkList?.add(position, value?.let { it1 -> SelectedModel(false, it1) }!!)

                    holder.clMain.setBackgroundResource(android.R.color.transparent)
                } else {
                    if (value != null) {
                        MySingelton.setSelectedVideos(value)
                    }
                    checkList?.removeAt(position)
                    checkList?.add(position, value?.let { it1 -> SelectedModel(true, it1) }!!)

                    holder.clMain.setBackgroundResource(R.color.purple_200)
                }
                for (i in 0 until checkList?.size!!) {
                    val check = checkList!![i].selected

                    if (check) {
                        (context.context as? ViewTypeInterface)?.setSaveCheckRes(true)
                        break
                    } else {
                        (context.context as? ViewTypeInterface)?.setSaveCheckRes(false)
                    }
                }
                for (i in 0 until checkList?.size!!) {
                    val check = checkList!![i].selected

                    if (!check) {
                        (context.context as? ViewTypeInterface)?.setSelectionCheckRes(false)
                    } else {
                        (context.context as? ViewTypeInterface)?.setSelectionCheckRes(true)
                    }
                }
                return@setOnLongClickListener true
            }
            holder.optionHolder.setOnClickListener() {
                val popupMenu: PopupMenu = PopupMenu(it.context, holder.optionHolder)
                popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { item ->
                    newPosition = position
                    newItem = videos

                    when (item.itemId) {
                        R.id.action_rename -> {
                            renameFunction(newPosition)
                        }
                        R.id.action_delete -> {
                            if (videos != null) {
                                deleteFunction(it, videos)
                            }
                        }
                    }
                    true
                }
                popupMenu.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        return if (items != null) {
            items!!.size
        } else {
            0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isSwitchView) {
            listItem
        } else {
            gridItem
        }
    }

    fun toggleItemViewType(): Boolean {
        isSwitchView = !isSwitchView
        return isSwitchView
    }

    fun getItemViewType(): Boolean {
        return isSwitchView
    }

    fun getSelectedItemsCheck(): Boolean {
        var checkVal: Boolean = false
        for (i in 0 until checkList?.size!!) {
            val check = checkList!![i].selected

            if (check) {
                checkVal = check
                break
            }
        }
        return checkVal
    }

    fun onResult(requestCode: Int) {
        when (requestCode) {
            123 -> afterDeletePermission(newPosition)
            124 -> afterRenamePermission(newPosition)
        }
    }

    private lateinit var newName: String

    private fun renameFunction(position: Int) {
        val dialog =
            Dialog(context.requireContext(), android.R.style.Theme_Material_Light_Dialog_Alert)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        dialog.setContentView(R.layout.rename_dialog_design)
        val name = dialog.findViewById(R.id.name) as EditText
        name.setText(items?.get(position)?.title)

        val ok = dialog.findViewById(R.id.ok) as Button
        ok.setOnClickListener {
            newName = name.text.toString()
            if (newName.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    dialog.dismiss()

                    val uriList: List<Uri> = listOf(
                        Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            items?.get(newPosition)?.id
                        )
                    )
                    val pi =
                        MediaStore.createWriteRequest(
                            context.requireContext().contentResolver,
                            uriList
                        )
                    (context.context as Activity).startIntentSenderForResult(
                        pi.intentSender, 124,
                        null, 0, 0, 0, null
                    )
                } else {
                    val currentFile = items?.get(position)?.path?.let { it1 -> File(it1) }
                    if (currentFile != null) {
                        if (currentFile.exists() && newName.toString()
                                .isNotEmpty()
                        ) {
                            val newFile = File(
                                currentFile.parentFile,
                                newName.toString() + "." + currentFile.extension
                            )
                            if (currentFile.renameTo(newFile)) {
                                MediaScannerConnection.scanFile(
                                    context.context,
                                    arrayOf(newFile.toString()),
                                    arrayOf("video/*"),
                                    null
                                )
                            }
                            updateRenameUI(
                                newItem,
                                position = position,
                                newName = newName.toString(),
                                newFile = newFile
                            )
                        }

                    }
                    dialog.dismiss()
                }
            } else {
                name.error = "Field required."
            }
        }

        dialog.show()
    }

    private fun afterRenamePermission(position: Int) {
        val currentFile = items?.get(position)?.path?.let { File(it) }
        if (currentFile != null) {
            if (currentFile.exists() && newName.toString()
                    .isNotEmpty()
            ) {
                val newFile = File(
                    currentFile.parentFile,
                    newName.toString() + "." + currentFile.extension
                )

                val fromUri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    items?.get(position)?.id
                )

                ContentValues().also {
                    it.put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                    context.requireContext().contentResolver.update(
                        fromUri,
                        it,
                        null,
                        null
                    )
                    it.clear()

                    //updating file details
                    it.put(
                        MediaStore.Files.FileColumns.DISPLAY_NAME,
                        newName.toString()
                    )
                    it.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                    context.requireContext().contentResolver.update(
                        fromUri,
                        it,
                        null,
                        null
                    )
                }

                updateRenameUI(
                    newItem,
                    position,
                    newName = newName.toString(),
                    newFile = newFile
                )
            }
        }
    }

    private fun updateRenameUI(newItem: MyModel?, position: Int, newName: String, newFile: File) {

        val newItem = MyModel(
            newItem?.id,
            newName,
            newItem?.folderName,
            newItem?.size,
            newFile.path,
            Uri.fromFile(newFile)
        )
        items?.removeAt(position)
        items?.add(position, newItem)
        notifyItemChanged(position)
    }

    private fun deleteFunction(v: View?, item: MyModel) {
        //list of videos to delete
        val uriList: List<Uri> = listOf(
            Uri.withAppendedPath(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                item.id
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //requesting for delete permission
            if (v != null) {
                val pi =
                    MediaStore.createDeleteRequest(v.context.contentResolver, uriList)

                (v.context as Activity).startIntentSenderForResult(
                    pi.intentSender, 123,
                    null, 0, 0, 0, null
                )
            }
        } else {
            //for devices less than android 11
            if (v != null) {
                val file = item.path?.let { File(it) }
                val builder = MaterialAlertDialogBuilder(v.context)
                builder.setTitle("Delete Video?")
                    .setMessage(item.title)
                    .setPositiveButton("Yes") { self, _ ->
                        if (file != null) {
                            if (file.exists() && file.delete()) {
                                MediaScannerConnection.scanFile(
                                    v.context,
                                    arrayOf(file.path),
                                    null,
                                    null
                                )
                                afterDeletePermission(newPosition)
                            }
                        }
                        self.dismiss()
                    }
                    .setNegativeButton("No") { self, _ -> self.dismiss() }
                val delDialog = builder.create()
                delDialog.show()
            }
        }
    }

    private fun afterDeletePermission(position: Int) {
        items?.removeAt(position)
        notifyDataSetChanged()
//        notifyItemChanged(position)
    }

    fun setListItems(items: ArrayList<MyModel>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun checkSelectedItems(selected: ArrayList<SelectedModel>) {
        checkList = selected
        notifyDataSetChanged()
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageHolder: ImageView = itemView.findViewById(R.id.imageView)
        val optionHolder: ImageView = itemView.findViewById(R.id.option)
        val nameHolder: TextView = itemView.findViewById(R.id.name)
        val fnameHolder: TextView = itemView.findViewById(R.id.foldername)
        val clMain: ConstraintLayout = itemView.findViewById(R.id.cl_main)
    }
}