package com.soundfusion.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundfusion.core.database.dao.DownloadDao
import com.soundfusion.core.database.entity.DownloadEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadDao: DownloadDao,
) : ViewModel() {

    val downloads: Flow<List<DownloadEntity>> = downloadDao.observeAll()

    fun removeDownload(id: String) {
        viewModelScope.launch {
            val entity = DownloadEntity(id = id, trackId = "", filePath = "")
            downloadDao.delete(entity)
        }
    }
}
