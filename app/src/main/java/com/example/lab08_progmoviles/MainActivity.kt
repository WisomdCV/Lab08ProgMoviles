package com.example.lab08_progmoviles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lab08_progmoviles.ui.theme.Lab08ProgMovilesTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08ProgMovilesTheme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                ).build()

                val taskDao = db.taskDao()
                val viewModel = TaskViewModel(taskDao)

                var newTaskDescription by remember { mutableStateOf("") }

                scheduleNotificationWorker()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Tareas") },
                            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFBBC6FB))
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            if (newTaskDescription.isNotEmpty()) {
                                viewModel.addTask(newTaskDescription)
                                newTaskDescription = ""
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar tarea")
                        }
                    },
                    bottomBar = {
                        BottomAppBar {
                            IconButton(onClick = { viewModel.showAllTasks() }) {
                                Icon(imageVector = Icons.Default.List, contentDescription = "Todas las tareas")
                            }
                            IconButton(onClick = { viewModel.showCompletedTasks() }) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Tareas completadas")
                            }
                            IconButton(onClick = { viewModel.showPendingTasks() }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Tareas pendientes")
                            }
                        }
                    }
                ) { innerPadding ->
                    TaskScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        newTaskDescription = newTaskDescription,
                        onTaskDescriptionChange = { newTaskDescription = it }
                    )
                }
            }
        }
    }

    private fun scheduleNotificationWorker() {
        val workManager = WorkManager.getInstance(applicationContext)
        val notificationWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            5, TimeUnit.MINUTES //Ejecutar cada 15
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "task_reminder_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            notificationWorkRequest
        )
    }
}

@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier,
    newTaskDescription: String,
    onTaskDescriptionChange: (String) -> Unit
) {
    val filteredTasks by viewModel.filteredTasks.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingTaskId by remember { mutableStateOf<Int?>(null) }
    var editedTaskDescription by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = newTaskDescription,
            onValueChange = onTaskDescriptionChange,
            label = { Text("Nueva tarea") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(filteredTasks) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (editingTaskId == task.id) {
                            TextField(
                                value = editedTaskDescription,
                                onValueChange = { editedTaskDescription = it },
                                label = { Text("Editar tarea") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {
                                viewModel.updateTaskDescription(task, editedTaskDescription)
                                editingTaskId = null
                                editedTaskDescription = ""
                            }) {
                                Text("Guardar")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.description,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                        .wrapContentWidth(Alignment.Start)
                                )
                                Row {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { viewModel.toggleTaskCompletion(task) }
                                    )
                                    IconButton(onClick = {
                                        editingTaskId = task.id
                                        editedTaskDescription = task.description
                                    }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar tarea")
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteTask(task)
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar tarea")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    viewModel.deleteAllTasks()
                    snackbarHostState.showSnackbar("Todas las tareas han sido eliminadas")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Eliminar todas las tareas")
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}

@Preview(showBackground = true)
@Composable
fun TaskScreenPreview() {
    Lab08ProgMovilesTheme {
        TaskScreen(
            viewModel = TaskViewModel(TODO()),
            newTaskDescription = "",
            onTaskDescriptionChange = {}
        )
    }
}