package com.willfp.libreforge.conditions.impl

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.conditions.Condition
import org.bukkit.entity.Player


object ConditionPlaceholderGreaterThan : Condition<NoCompileData>("placeholder_greater_than") {
    override val arguments = arguments {
        require("placeholder", "You must specify the placeholder!")
        require("value", "You must specify the value!")
    }

    override fun isMet(player: Player, config: Config, compileData: NoCompileData): Boolean {
        val value = config.getFormattedString("placeholder", placeholderContext(player = player))
            .toDoubleOrNull() ?: 0.0
        return value >= config.getDoubleFromExpression("value", player)
    }
}
