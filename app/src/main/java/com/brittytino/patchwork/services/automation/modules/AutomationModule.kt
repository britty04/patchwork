package com.brittytino.patchwork.services.automation.modules

import android.content.Context
import com.brittytino.patchwork.domain.diy.Automation

interface AutomationModule {
    val id: String
    fun start(context: Context)
    fun stop(context: Context)
    fun updateAutomations(automations: List<Automation>)
}
