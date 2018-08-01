// Collection of function to convert subsonic api jukebox responses to app entities
@file:JvmName("APIJukeboxConverter")
package pt.ipleiria.mymusicqoe.domain

import org.moire.ultrasonic.domain.JukeboxStatus
import org.moire.ultrasonic.api.subsonic.models.JukeboxStatus as ApiJukeboxStatus

fun ApiJukeboxStatus.toDomainEntity(): JukeboxStatus = JukeboxStatus(
        positionSeconds = this@toDomainEntity.position,
        currentPlayingIndex = this@toDomainEntity.currentIndex,
        isPlaying = this@toDomainEntity.playing,
        gain = this@toDomainEntity.gain
)
