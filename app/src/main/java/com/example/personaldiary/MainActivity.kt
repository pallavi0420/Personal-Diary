package com.example.personaldiary

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import com.example.personaldiary.ui.theme.PersonalDiaryTheme
import kotlinx.coroutines.launch

class Roomdaodemo : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PersonalDiaryTheme {
                DiaryScreen()
            }
        }
    }
}

// ENTITY
@Entity
data class diary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String
)

// DAO
@Dao
interface DiaryDataOperations {

    @Insert
    suspend fun insertDiaryData(d: diary)

    @Query("SELECT * FROM diary")
    suspend fun fetchDiaryData(): List<diary>

    @Query("SELECT COUNT(*) FROM diary")
    suspend fun getEntryCount(): Int

    @Delete
    suspend fun deleteDiaryData(d: diary)
}

// DATABASE
@Database(
    entities = [diary::class],
    version = 1
)
abstract class DiaryDataAbstraction : RoomDatabase() {
    abstract fun diaryDataDaoOperation(): DiaryDataOperations
}

// DATABASE OBJECT
object DiaryDataDB {

    fun getDiaryDB(c: Context): DiaryDataAbstraction {
        return Room.databaseBuilder(
            c,
            DiaryDataAbstraction::class.java,
            "DiaryDB"
        ).build()
    }
}

// VIEWMODEL
class DiaryRoomArchi(a: Application) : AndroidViewModel(a) {

    val db = DiaryDataDB.getDiaryDB(a)

    var diaryList by mutableStateOf<List<diary>>(emptyList())
        private set

    var message by mutableStateOf("")

    // INSERT FUNCTION
    fun insertDiaryData(title: String, content: String) {

        viewModelScope.launch {

            val count = db.diaryDataDaoOperation().getEntryCount()

            if (count < 3) {

                db.diaryDataDaoOperation().insertDiaryData(
                    diary(title = title, content = content)
                )

                message = "Diary entry added successfully!"

                getDiaryData()

            } else {

                message = "Maximum 3 entries allowed!"
            }
        }
    }

    // QUERY FUNCTION
    fun getDiaryData() {

        viewModelScope.launch {

            diaryList =
                db.diaryDataDaoOperation().fetchDiaryData()
        }
    }

    // DELETE FUNCTION
    fun deleteDiaryData(d: diary) {

        viewModelScope.launch {

            db.diaryDataDaoOperation().deleteDiaryData(d)

            message = "Diary entry deleted!"

            getDiaryData()
        }
    }
}

// COMPOSABLE SCREEN
@Composable
fun DiaryScreen(viewModel: DiaryRoomArchi = viewModel()) {

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.getDiaryData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp)
    ) {

        Text("PERSONAL DIARY")

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Enter Title") }
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Enter Content") }
        )

        Button(
            onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    viewModel.insertDiaryData(title, content)
                    title = ""
                    content = ""
                }
            }
        ) {
            Text("Add Entry")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(viewModel.message)

        Text("Diary Entries: ${viewModel.diaryList.size}/3")

        LazyColumn {

            items(viewModel.diaryList) { diaryItem ->

                Text(
                    text = "ID: ${diaryItem.id}",
                    modifier = Modifier.padding(8.dp)
                )

                Text(
                    text = "Title: ${diaryItem.title}",
                    modifier = Modifier.padding(8.dp)
                )

                Text(
                    text = "Content: ${diaryItem.content}",
                    modifier = Modifier.padding(8.dp)
                )

                Button(
                    onClick = {
                        viewModel.deleteDiaryData(diaryItem)
                    }
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
