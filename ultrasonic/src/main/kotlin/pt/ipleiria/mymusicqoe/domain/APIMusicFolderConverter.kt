// Converts MusicFolder entity from [org.moire.ultrasonic.api.subsonic.SubsonicAPIClient]
// to app domain entities.
@file:JvmName("APIMusicFolderConverter")
package pt.ipleiria.mymusicqoe.domain

import org.moire.ultrasonic.domain.MusicFolder
import org.moire.ultrasonic.api.subsonic.models.MusicFolder as APIMusicFolder

fun APIMusicFolder.toDomainEntity(): MusicFolder = MusicFolder(this.id, this.name)

fun List<APIMusicFolder>.toDomainEntityList(): List<MusicFolder> =
        this.map { it.toDomainEntity() }
