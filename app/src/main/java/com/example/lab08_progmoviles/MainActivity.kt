package com.example.lab08_progmoviles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.lab08_progmoviles.ui.theme.Lab08ProgMovilesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            if (newTaskDescription.isNotEmpty()) {
                                viewModel.addTask(newTaskDescription)
                                newTaskDescription = ""
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar tarea")
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
}

@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier,
    newTaskDescription: String,
    onTaskDescriptionChange: (String) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

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

        tasks.forEach { task ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = task.description)
                Button(onClick = { viewModel.toggleTaskCompletion(task) }) {
                    Text(if (task.isCompleted) "Completada" else "Pendiente")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { coroutineScope.launch { viewModel.deleteAllTasks() } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Eliminar todas las tareas")
        }
    }
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
