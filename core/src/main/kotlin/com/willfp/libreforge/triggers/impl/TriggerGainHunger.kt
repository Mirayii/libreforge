package com.willfp.libreforge.triggers.impl

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.FoodLevelChangeEvent

object TriggerGainHunger : Trigger("gain_hunger") {
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.EVENT
    )

    @EventHandler(ignoreCancelled = true)
    fun handle(event: FoodLevelChangeEvent) {
        val player = event.entity

        if (player !is Player) {
            return
        }

        if (event.foodLevel < player.foodLevel) {
            return
        }

        this.dispatch(
            player,
            TriggerData(
                player = player,
                event = event,
                value = event.foodLevel.toDouble() - player.foodLevel
            )
        )
    }
}
