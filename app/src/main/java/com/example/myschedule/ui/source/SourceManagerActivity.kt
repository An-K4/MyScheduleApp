package com.example.myschedule.ui.source

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myschedule.data.entity.CalendarSource
import com.example.myschedule.databinding.ActivitySourceManagerBinding
import com.example.myschedule.viewmodel.SourceManagerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SourceManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySourceManagerBinding
    private val viewModel: SourceManagerViewModel by viewModels()
    private lateinit var sourceAdapter: SourceAdapter

    // 5.5 — File picker để import .ics mới
    private val selectIcsFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val fileName = getFileName(it)
                viewModel.importIcsFile(it, fileName)
                Toast.makeText(this, "Đã nhập: $fileName", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySourceManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        sourceAdapter = SourceAdapter(
            // 5.6 — Toggle checkbox → cập nhật DB
            onToggle = { source, isEnabled ->
                viewModel.toggleSource(source.id, isEnabled)
            },
            // 5.7 — Xóa nguồn → xác nhận → xóa source + cascade events
            onDelete = { source ->
                showDeleteConfirmDialog(source)
            }
        )

        binding.rvSources.apply {
            adapter = sourceAdapter
            layoutManager = LinearLayoutManager(this@SourceManagerActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.allSources.observe(this) { sources ->
            sourceAdapter.submitList(sources)

            // Hiện/ẩn empty state
            if (sources.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvSources.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvSources.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        // 5.5 — FAB mở file picker
        binding.fabAddSource.setOnClickListener {
            selectIcsFileLauncher.launch(
                arrayOf("text/calendar", "application/octet-stream")
            )
        }
    }

    // 5.7 — Dialog xác nhận xóa
    private fun showDeleteConfirmDialog(source: CalendarSource) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa nguồn lịch?")
            .setMessage("Tất cả sự kiện từ \"${source.name}\" sẽ bị xóa vĩnh viễn.")
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteSource(source)
                Toast.makeText(this, "Đã xóa \"${source.name}\"", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun getFileName(uri: Uri): String {
        var name = "Lịch mới"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx != -1) name = cursor.getString(idx).removeSuffix(".ics")
            }
        }
        return name
    }
}