package com.demo.tasks

import Task
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.demo.tasks.db.AppDatabase
import com.demo.tasks.db.TaskDao
import com.demo.tasks.ui.theme.TasksTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "task-db"
        ).build()

        val taskDao = db.taskDao()
        viewModel = ViewModelProvider(this, MainViewModelFactory(taskDao))[MainViewModel::class.java]

        setContent {
            TasksTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ToDoListApp(viewModel)
                }
            }
        }
    }
}

class MainViewModelFactory(private val taskDao: TaskDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(taskDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoListApp() {
    var taskName by remember { mutableStateOf(TextFieldValue()) }
    var dueDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var isEditing by remember { mutableStateOf(false) }
    var editTaskId by remember { mutableStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do List") }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Task Input
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                DatePicker(
                    selectedDate = dueDate,
                    onDateSelected = { dueDate = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        if (isEditing) {
                            tasks.find { it.id == editTaskId }?.apply {
                                name = taskName.text
                                this.dueDate = dueDate
                            }
                            isEditing = false
                            taskName = TextFieldValue("")
                        } else {
                            tasks.add(Task(taskId++, taskName.text, dueDate))
                            taskName = TextFieldValue("")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(if (isEditing) "Edit Task" else "Add Task")
                }

                // Task List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(tasks) { task ->
                        TaskItem(
                            task = task,
                            onTaskClick = {
                                taskName = TextFieldValue(task.name)
                                dueDate = task.dueDate
                                isEditing = true
                                editTaskId = task.id
                            },
                            onCheckboxClick = { task.isCompleted = !task.isCompleted },
                            onDeleteClick = {
                                tasks.remove(task)
                            }
                        )
                    }
                }

                // Delete All Completed Tasks Button
                Button(
                    onClick = {
                        tasks.removeAll { it.isCompleted }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete All Completed Tasks")
                }
            }
        }
    )
}

@Composable
fun TaskItem(
    task: Task,
    onTaskClick: () -> Unit,
    onCheckboxClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onTaskClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onCheckboxClick() },
            modifier = Modifier.padding(end = 8.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = task.name, fontSize = 18.sp)
            Text(
                text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.dueDate),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Task",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePicker(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    var datePickerDialogShown by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = TextFieldValue(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)
        ),
        onValueChange = {},
        label = { Text("Due Date") },
        modifier = modifier.clickable { datePickerDialogShown = true }
    )

    if (datePickerDialogShown) {
        // You can use a DatePicker library here or implement your own DatePicker dialog
        // For simplicity, I'm omitting the implementation of the DatePicker dialog
    }
}
