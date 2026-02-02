package com.brittytino.patchwork.services.automation.executors

import android.content.Context
import com.brittytino.patchwork.domain.diy.Action

interface ActionExecutor {
    suspend fun execute(context: Context, action: Action)
}
