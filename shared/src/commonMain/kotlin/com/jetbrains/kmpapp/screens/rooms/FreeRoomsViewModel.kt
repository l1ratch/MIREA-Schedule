package com.jetbrains.kmpapp.screens.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.FreeRoomsRepository
import com.jetbrains.kmpapp.data.model.DateUtils
import com.jetbrains.kmpapp.data.model.FreeRoomItem
import com.jetbrains.kmpapp.data.model.FreeRoomsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FreeRoomsViewModel(
    private val repository: FreeRoomsRepository
) : ViewModel() {

    private val _freeRoomsData = MutableStateFlow(FreeRoomsData())
    val freeRoomsData: StateFlow<FreeRoomsData> = _freeRoomsData.asStateFlow()

    private val _selectedCampus = MutableStateFlow("В-78")
    val selectedCampus: StateFlow<String> = _selectedCampus.asStateFlow()

    private val _selectedFloor = MutableStateFlow<Int?>(null)
    val selectedFloor: StateFlow<Int?> = _selectedFloor.asStateFlow()

    private val _selectedBell = MutableStateFlow(calculateCurrentBell())
    val selectedBell: StateFlow<Int> = _selectedBell.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedRoomForDetail = MutableStateFlow<FreeRoomItem?>(null)
    val selectedRoomForDetail: StateFlow<FreeRoomItem?> = _selectedRoomForDetail.asStateFlow()

    val currentDateIso: String = getCurrentDateIso()

    val availableCampuses: StateFlow<List<String>> = combine(_freeRoomsData) { data ->
        val list = data[0].campuses
        if (list.isNotEmpty()) list else listOf("В-78", "В-86", "С-20", "СГ-22", "МП-1")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("В-78", "В-86", "С-20", "СГ-22", "МП-1"))

    val availableFloors: StateFlow<List<Int>> = combine(_freeRoomsData, _selectedCampus) { data, campus ->
        data.rooms
            .filter { it.campus.equals(campus, ignoreCase = true) }
            .mapNotNull { it.floor }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRooms: StateFlow<List<FreeRoomItem>> = combine(
        _freeRoomsData,
        _selectedCampus,
        _selectedFloor,
        _selectedBell,
        _searchQuery
    ) { data, campus, floor, bell, query ->
        repository.filterFreeRooms(
            allRooms = data.rooms,
            campus = campus,
            floor = floor,
            dateIso = currentDateIso,
            bellNumber = bell,
            searchQuery = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadFreeRooms(forceRefresh = false)
    }

    fun loadFreeRooms(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _isRefreshing.value = true
            } else {
                _isLoading.value = true
            }

            val data = repository.getFreeRooms(forceRefresh = forceRefresh)
            _freeRoomsData.value = data

            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    fun selectCampus(campus: String) {
        _selectedCampus.value = campus
        _selectedFloor.value = null // reset floor filter when campus changes
    }

    fun selectFloor(floor: Int?) {
        _selectedFloor.value = floor
    }

    fun selectBell(bell: Int) {
        _selectedBell.value = bell
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectRoomForDetail(room: FreeRoomItem?) {
        _selectedRoomForDetail.value = room
    }

    private companion object {
        fun getCurrentDateIso(): String = DateUtils.today().toString()

        fun calculateCurrentBell(): Int {
            val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val hhmm = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
            return when {
                hhmm < "10:35" -> 1 // 09:00 - 10:30
                hhmm < "12:20" -> 2 // 10:40 - 12:10
                hhmm < "14:15" -> 3 // 12:40 - 14:10
                hhmm < "16:00" -> 4 // 14:20 - 15:50
                hhmm < "18:00" -> 5 // 16:20 - 17:50
                hhmm < "19:35" -> 6 // 18:00 - 19:30
                hhmm < "21:15" -> 7 // 19:40 - 21:10
                else -> 1
            }
        }
    }
}
