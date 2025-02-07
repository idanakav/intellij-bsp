package org.jetbrains.plugins.bsp.ui.configuration.test

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.server.tasks.TestTargetTask
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.targetIdTOREMOVE
import javax.swing.Icon

public class BspConfigurationType : ConfigurationType {
  override fun getDisplayName(): String = "BSP TEST"

  override fun getConfigurationTypeDescription(): String = "BSP TEST"

  override fun getIcon(): Icon = BspPluginIcons.bsp

  override fun getId(): String = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> =
    arrayOf(TestRunFactory(this))

  public companion object {
    public const val ID: String = "BSP_TEST_RUN_CONFIGURATION"
  }
}

public class TestRunFactory(t: ConfigurationType) : ConfigurationFactory(t) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration =
    TestRunConfiguration(project, this, "BSP TEST")

  override fun getId(): String =
    BspConfigurationType.ID
}

public class TestRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String) :
  RunConfigurationBase<String>(project, configurationFactory, name) {
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return RunProfileState { executor2, _ ->

      val bspTestConsole = BspConsoleService.getInstance(project).bspTestConsole

      val processHandler = BspProcessHandler()
      val testConsole = BspTestConsolePrinter(processHandler, SMTRunnerConsoleProperties(this, "BSP", executor2))
      environment.getUserData(targetIdTOREMOVE)?.let {
        bspTestConsole.registerPrinter(testConsole)
        processHandler.execute {
          try {
            // TODO error handling?
            TestTargetTask(project).connectAndExecute(it.toBsp4JTargetIdentifier())
          } finally {
            testConsole.endTesting()
            bspTestConsole.deregisterPrinter(testConsole)
          }
        }
      } ?: processHandler.shutdown()
      DefaultExecutionResult(testConsole.console, processHandler)
    }
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    TODO("Not yet implemented")
  }
}
