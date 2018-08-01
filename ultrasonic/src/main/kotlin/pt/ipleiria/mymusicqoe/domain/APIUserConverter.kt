// Helper functions to convert User entity to domain entity
@file:JvmName("APIUserConverter")
package pt.ipleiria.mymusicqoe.domain

import org.moire.ultrasonic.api.subsonic.models.User
import org.moire.ultrasonic.domain.UserInfo

fun User.toDomainEntity(): UserInfo = UserInfo(
        adminRole = this@toDomainEntity.adminRole,
        commentRole = this@toDomainEntity.commentRole,
        coverArtRole = this@toDomainEntity.coverArtRole,
        downloadRole = this@toDomainEntity.downloadRole,
        email = this@toDomainEntity.email,
        jukeboxRole = this@toDomainEntity.jukeboxRole,
        playlistRole = this@toDomainEntity.playlistRole,
        podcastRole = this@toDomainEntity.podcastRole,
        scrobblingEnabled = this@toDomainEntity.scrobblingEnabled,
        settingsRole = this@toDomainEntity.settingsRole,
        shareRole = this@toDomainEntity.shareRole,
        streamRole = this@toDomainEntity.streamRole,
        uploadRole = this@toDomainEntity.uploadRole,
        userName = this@toDomainEntity.username
)
