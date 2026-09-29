package com.example.kuis1studentmanager

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

data class Student(
    val id: String = UUID.randomUUID().toString(),
    val nim: String,
    val name: String,
    val programStudi: String
)

class StudentViewModel : ViewModel() {
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun getFilteredList(): List<Student> {
        val query = _searchQuery.value
        return if (query.isBlank()) {
            _students.value
        } else {
            _students.value.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.nim.contains(query, ignoreCase = true)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addStudent(nim: String, name: String, prodi: String) {
        _students.update { it + Student(nim = nim, name = name, programStudi = prodi) }
    }

    fun updateStudent(id: String, nim: String, name: String, prodi: String) {
        _students.update { list ->
            list.map { if (it.id == id) it.copy(nim = nim, name = name, programStudi = prodi) else it }
        }
    }

    fun deleteStudent(id: String) {
        _students.update { list -> list.filter { it.id != id } }
    }

    fun getStudentById(id: String): Student? {
        return _students.value.find { it.id == id }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudentApp()
                }
            }
        }
    }
}

@Composable
fun StudentApp(viewModel: StudentViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("home") { HomeScreen(navController, viewModel) }
        composable("add") { AddEditScreen(navController, viewModel, null) }
        composable(
            route = "edit/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry: NavBackStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId")
            AddEditScreen(navController, viewModel, studentId)
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(2.seconds)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Student Manager", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Kelola data mahasiswa\ndengan mudah", textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: StudentViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val studentList = viewModel.getFilteredList()
    var showMenu by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Manager") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Tentang Aplikasi") }, onClick = { showMenu = false })
                        DropdownMenuItem(
                            text = { Text("Keluar") },
                            onClick = {
                                showMenu = false
                                (context as? Activity)?.finish()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add") },
                containerColor = Color(0xFF0D6EFD),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Cari mahasiswa...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = CircleShape,
                singleLine = true
            )

            Text(
                text = "Jumlah mahasiswa: ${studentList.size}",
                modifier = Modifier.padding(horizontal = 16.dp),
                fontWeight = FontWeight.SemiBold
            )

            if (studentList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Belum ada data mahasiswa", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Silakan tambah untuk\nmenambahkan mahasiswa pertama", textAlign = TextAlign.Center, color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(studentList) { student ->
                        StudentCard(
                            student = student,
                            onEditClick = { navController.navigate("edit/${student.id}") },
                            onDeleteClick = { studentToDelete = student }
                        )
                    }
                }
            }
        }
    }

    if (studentToDelete != null) {
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Hapus Mahasiswa?") },
            text = { Text("Apakah anda yakin ingin menghapus data mahasiswa ini?") },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(studentToDelete!!.id)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { studentToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun StudentCard(student: Student, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, fontWeight = FontWeight.Bold)
                Text("NIM: ${student.nim}", fontSize = 12.sp, color = Color.Gray)
                Text(student.programStudi, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(navController: NavController, viewModel: StudentViewModel, studentId: String?) {
    val studentToEdit = studentId?.let { viewModel.getStudentById(it) }

    var nim by remember { mutableStateOf(studentToEdit?.nim ?: "") }
    var name by remember { mutableStateOf(studentToEdit?.name ?: "") }
    var programStudi by remember { mutableStateOf(studentToEdit?.programStudi ?: "") }

    val prodiOptions = listOf("Informatika", "Sistem Informasi", "Teknik Komputer", "Desain Komunikasi Visual", "Manajemen", "Akuntansi")
    var expandedProdi by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (studentId != null) "Edit Mahasiswa" else "Tambah Mahasiswa") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            OutlinedTextField(
                value = nim,
                onValueChange = { nim = it },
                label = { Text("NIM") },
                placeholder = { Text("Masukkan NIM") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama") },
                placeholder = { Text("Masukkan nama mahasiswa") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expandedProdi,
                onExpandedChange = { expandedProdi = !expandedProdi }
            ) {
                OutlinedTextField(
                    value = programStudi,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Program Studi") },
                    placeholder = { Text("Pilih program studi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProdi) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedProdi,
                    onDismissRequest = { expandedProdi = false }
                ) {
                    prodiOptions.forEach { prodi ->
                        DropdownMenuItem(
                            text = { Text(prodi) },
                            onClick = {
                                programStudi = prodi
                                expandedProdi = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        if (studentId != null) {
                            viewModel.updateStudent(studentId, nim, name, programStudi)
                        } else {
                            viewModel.addStudent(nim, name, programStudi)
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D6EFD)),
                    enabled = nim.isNotBlank() && name.isNotBlank() && programStudi.isNotBlank()
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}