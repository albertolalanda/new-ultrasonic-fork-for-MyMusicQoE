package org.moire.ultrasonic.api.subsonic.response

import org.moire.ultrasonic.api.subsonic.SubsonicAPIVersions
import org.moire.ultrasonic.api.subsonic.SubsonicError
import org.moire.ultrasonic.api.subsonic.models.LastIdUser

class GetUserLastIdResponse(
        val lastIdUser: LastIdUser = LastIdUser(),
        status: Status,
        version: SubsonicAPIVersions,
        error: SubsonicError?
) : SubsonicResponse(status, version, error)
