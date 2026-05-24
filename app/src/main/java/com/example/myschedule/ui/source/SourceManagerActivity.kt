package com.example.myschedule.ui.source

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myschedule.data.entity.CalendarSource
import com.example.myschedule.data.repository.ImportResult
import com.example.myschedule.databinding.ActivitySourceManagerBinding
import com.example.myschedule.ui.base.BaseActivity
import com.example.myschedule.viewmodel.SourceManagerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SourceManagerActivity : BaseActivity() {

    private lateinit var binding: ActivitySourceManagerBinding
    private val viewModel: SourceManagerViewModel by viewModels()
    private lateinit var sourceAdapter: SourceAdapter

    private val selectIcsFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.importIcsFile(it, getFileName(it))
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
            onToggle = { source, isEnabled -> viewModel.toggleSource(source.id, isEnabled) },
            onDelete = { source -> showDeleteConfirmDialog(source) }
        )
        binding.rvSources.apply {
            adapter = sourceAdapter
            layoutManager = LinearLayoutManager(this@SourceManagerActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.allSources.observe(this) { sources ->
            sourceAdapter.submitList(sources)
            binding.layoutEmpty.visibility = if (sources.isEmpty()) View.VISIBLE else View.GONE
            binding.rvSources.visibility = if (sources.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.importResult.observe(this) { result ->
            result ?: return@observe
            when (result) {
                is ImportResult.Success ->
                    Toast.makeText(
                        this,
                        "Đã nhập \"${result.source.name}\" — ${result.eventCount} sự kiện",
                        Toast.LENGTH_SHORT
                    ).show()
                is ImportResult.Duplicate ->
                    Toast.makeText(
                        this,
                        "\"${result.existingSource.name}\" đã được nhập trước đó",
                        Toast.LENGTH_SHORT
                    ).show()
                is ImportResult.Error ->
                    Toast.makeText(this, "Lỗi: ${result.message}", Toast.LENGTH_LONG).show()
            }
            viewModel.clearImportResult()
        }
    }

    private fun setupClickListeners() {
        binding.fabAddSource.setOnClickListener {
            selectIcsFileLauncher.launch(arrayOf("text/calendar", "application/octet-stream"))
        }
    }

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