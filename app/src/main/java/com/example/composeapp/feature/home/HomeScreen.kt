package com.example.composeapp.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateProject: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MoreCut", style = MaterialTheme.typography.headlineMedium)
                Text("Professional editing, offline first.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onOpenSettings) { Text("⚙", style = MaterialTheme.typography.titleLarge) }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("OFFLINE READY", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text("Your local projects remain available without internet.")
            }
        }

        Button(onClick = onCreateProject, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Create new project")
        }

        OutlinedButton(onClick = onOpenProjects, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Projects (${state.projectCount})")
        }

        Spacer(Modifier.height(8.dp))
        if (state.hasRecentProjects) {
            Text("${state.projectCount} local project${if (state.projectCount == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium)
            Text("Project metadata and versioned documents are stored on-device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("No projects yet", style = MaterialTheme.typography.titleMedium)
            Text("Create a project to start building your first timeline.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
