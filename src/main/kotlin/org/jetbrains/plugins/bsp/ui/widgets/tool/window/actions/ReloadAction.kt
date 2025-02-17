package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.tasks.SyncProjectTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class ReloadAction :
  AnAction({ BspAllTargetsWidgetBundle.message("reload.action.text") }, BspPluginIcons.reload) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      BspCoroutineService.getInstance(project).start { doAction(project) }
    } else {
      log.warn("ReloadAction cannot be performed! Project not available.")
    }
  }

  private suspend fun doAction(project: Project) {
    SyncProjectTask(project).execute(
      shouldBuildProject = false,
      shouldReloadConnection = true,
    )
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doUpdate(e, project)
    } else {
      log.warn("ReloadAction cannot be updated! Project not available.")
    }
  }

  private fun doUpdate(e: AnActionEvent, project: Project) {
    e.presentation.isVisible = project.isBspProject
    e.presentation.isEnabled = shouldBeEnabled(project)
  }

  private fun shouldBeEnabled(project: Project): Boolean {
    val isConnected = BspConnectionService.getInstance(project).value != null

    return project.isBspProject && isConnected && !project.isSyncInProgress()
  }

  private fun Project.isSyncInProgress() =
    BspConsoleService.getInstance(this).bspSyncConsole.hasTasksInProgress()

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.BGT

  private companion object {
    private val log = logger<ReloadAction>()
  }
}
