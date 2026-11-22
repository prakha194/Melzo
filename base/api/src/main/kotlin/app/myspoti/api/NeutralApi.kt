/* SPDX-License-Identifier: GPL-3.0-only */
package app.myspoti.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/** What no music service of the app offers: the call has nothing to answer with. */
class NotAvailable(what: String) : java.io.IOException("$what: not available")

/**
 * The app's base platform: no music service behind it, and nothing sent anywhere. Searches,
 * albums and artists come from the metadata providers layered over it (the catalog module), the
 * sound from the phone's files, Live Radio and the extensions the user installs, the library from
 * the phone. What only a service account could give (its home, its playlists, its people) is
 * empty here, and changing it fails with [NotAvailable].
 */
class NeutralApi : MyspotiApi {
    private fun <T> none(offset: Int = 0) = Page<T>(emptyList(), offset, 0, null)
    private fun no(what: String): Nothing = throw NotAvailable(what)

    override val ready: StateFlow<Boolean> = MutableStateFlow(true)

    override val player: Player = object : Player {
        override val state: StateFlow<PlayerState?> = MutableStateFlow(null)
        override val queue: StateFlow<Queue?> = MutableStateFlow(null)
        override suspend fun play(contextUri: Uri, trackUri: Uri?, index: Int?, positionMs: Long?, order: List<Uri>?) = no("play")
        override suspend fun pause() {}
        override suspend fun resume() {}
        override suspend fun seek(positionMs: Long) {}
        override suspend fun next() {}
        override suspend fun previous() {}
        override suspend fun setShuffle(on: Boolean) {}
        override suspend fun setRepeat(mode: Repeat) {}
        override suspend fun addToQueue(uris: List<Uri>) = no("queue")
        override suspend fun removeFromQueue(items: List<NowPlaying>) {}
        override suspend fun clearQueue() {}
        override suspend fun skipTo(item: NowPlaying) {}
        override suspend fun playNext(uris: List<Uri>) = no("queue")
        override suspend fun insertInQueue(uris: List<Uri>, before: NowPlaying) = no("queue")
        override suspend fun moveInQueue(item: NowPlaying, target: NowPlaying, after: Boolean) {}
        override suspend fun volume(): Double = 1.0
        override suspend fun setVolume(level: Double) {}
        override suspend fun setSpeed(speed: Double) {}
    }

    override val search: Search = object : Search {
        override suspend fun all(query: String, limit: Int) = SearchResults(none(), none(), none(), none())
        override suspend fun tracks(query: String, offset: Int, limit: Int) = none<Track>(offset)
        override suspend fun artists(query: String, offset: Int, limit: Int) = none<Artist>(offset)
        override suspend fun albums(query: String, offset: Int, limit: Int) = none<Album>(offset)
        override suspend fun playlists(query: String, offset: Int, limit: Int) = none<Playlist>(offset)
        override suspend fun shows(query: String, offset: Int, limit: Int) = none<MediaCard>(offset)
        override suspend fun episodes(query: String, offset: Int, limit: Int) = none<Episode>(offset)
        override suspend fun audiobooks(query: String, offset: Int, limit: Int) = none<MediaCard>(offset)
        override suspend fun users(query: String, offset: Int, limit: Int) = none<MediaCard>(offset)
        override suspend fun genres(query: String, offset: Int, limit: Int) = none<MediaCard>(offset)
        override suspend fun suggestions(query: String, limit: Int): List<String> = emptyList()
        override suspend fun recent(): List<MediaCard> = emptyList()
        override suspend fun clearRecent() {}
    }

    override val catalog: Catalog = object : Catalog {
        override suspend fun track(uri: Uri): Track = no(uri)
        override suspend fun album(uri: Uri): Album = no(uri)
        override suspend fun albumTracks(uri: Uri, offset: Int, limit: Int): Page<Track> = no(uri)
        override suspend fun artist(uri: Uri): Artist = no(uri)
        override suspend fun artistTopTracks(uri: Uri): List<Track> = emptyList()
        override suspend fun artistPage(uri: Uri): ArtistPage = no(uri)
        override suspend fun isrc(trackUri: Uri): String? = null
        override suspend fun discography(uri: Uri, offset: Int, limit: Int) = none<Album>(offset)
        override suspend fun relatedArtists(uri: Uri): List<Artist> = emptyList()
        override suspend fun appearsOn(uri: Uri): List<Album> = emptyList()
        override suspend fun similarAlbums(trackUri: Uri, limit: Int): List<Album> = emptyList()
    }

    override val library: Library = object : Library {
        override suspend fun contains(uris: List<Uri>): List<Boolean> = uris.map { false }
        override suspend fun add(uris: List<Uri>) = no("library")
        override suspend fun remove(uris: List<Uri>) = no("library")
        override suspend fun items(filter: LibraryFilter, sort: LibrarySort, folderUri: Uri?, offset: Int, limit: Int) = none<LibraryItem>(offset)
        override suspend fun likedTracks(offset: Int, limit: Int) = none<SavedTrack>(offset)
        override suspend fun pin(uri: Uri) = no("library")
        override suspend fun unpin(uri: Uri) = no("library")
        override suspend fun createFolder(name: String, parentFolderUri: Uri?): Uri = no("library")
        override suspend fun renameFolder(folderUri: Uri, name: String) = no("library")
        override suspend fun deleteFolder(folderUri: Uri) = no("library")
        override suspend fun moveToFolder(uri: Uri, folderUri: Uri?) = no("library")
        override val changes: Flow<Unit> = emptyFlow()
    }

    override val playlists: Playlists = object : Playlists {
        override suspend fun playlist(uri: Uri): Playlist = no(uri)
        override suspend fun tracks(uri: Uri, offset: Int, limit: Int): Page<Track> = no(uri)
        override suspend fun sortedTracks(uri: Uri, sort: PlaylistSort, descending: Boolean): List<Track> = no(uri)
        override suspend fun mine(): List<Playlist> = emptyList()
        override suspend fun editable(): List<Playlist> = emptyList()
        override suspend fun create(name: String, folderUri: Uri?): Uri = no("playlists")
        override suspend fun rename(uri: Uri, name: String) = no(uri)
        override suspend fun setDescription(uri: Uri, description: String) = no(uri)
        override suspend fun access(uri: Uri): PlaylistAccess = no(uri)
        override suspend fun setAccess(uri: Uri, access: PlaylistAccess) = no(uri)
        override suspend fun published(uri: Uri): Boolean = false
        override suspend fun setPublished(uri: Uri, published: Boolean) = no(uri)
        override suspend fun setCover(uri: Uri, jpeg: ByteArray?) = no(uri)
        override suspend fun delete(uri: Uri) = no(uri)
        override suspend fun follow(uri: Uri) = no(uri)
        override suspend fun unfollow(uri: Uri) = no(uri)
        override suspend fun join(uri: Uri, invitationToken: String): PlaylistAccess = no(uri)
        override suspend fun collaboratorInvite(uri: Uri): String = no(uri)
        override suspend fun members(uri: Uri): List<PlaylistMember> = emptyList()
        override suspend fun leave(uri: Uri) = no(uri)
        override suspend fun setMemberCollaborator(uri: Uri, username: String, collaborator: Boolean) = no(uri)
        override suspend fun removeMember(uri: Uri, username: String) = no(uri)
        override suspend fun stopCollaboration(uri: Uri) = no(uri)
        override suspend fun addedBy(uri: Uri): List<RowAdder> = emptyList()
        override suspend fun addTracks(uri: Uri, trackUris: List<Uri>) = no(uri)
        override suspend fun removeTracks(uri: Uri, uids: List<String>) = no(uri)
        override suspend fun moveTracks(uri: Uri, uids: List<String>, beforeUid: String?) = no(uri)
        override suspend fun recommended(uri: Uri, limit: Int, skip: List<Uri>): List<Track> = emptyList()
    }

    override val home: Home = object : Home {
        override suspend fun feed(filter: HomeFilter, itemsPerShelf: Int) = HomeFeed("", emptyList(), emptyList())
        override suspend fun categories(): List<Category> = emptyList()
        override suspend fun category(uri: Uri): BrowsePage = no(uri)
        override suspend fun whatsNew(offset: Int, limit: Int) = none<Album>(offset)
        override suspend fun whatsNewFeed(offset: Int, limit: Int, content: WhatsNewContent) = none<WhatsNewItem>(offset)
        override suspend fun markWhatsNewSeen(ids: List<String>) {}
    }

    override val podcasts: Podcasts = object : Podcasts {
        override suspend fun show(uri: Uri): Show = no(uri)
        override suspend fun audioUrl(uri: Uri): EpisodeAudio = no(uri)
        override suspend fun episodes(uri: Uri, offset: Int, limit: Int): Page<Episode> = no(uri)
        override suspend fun episode(uri: Uri): Episode = no(uri)
        override suspend fun audiobook(uri: Uri): Audiobook = no(uri)
        override suspend fun chapters(uri: Uri, offset: Int, limit: Int): Page<Episode> = no(uri)
        override suspend fun savedEpisodes(offset: Int, limit: Int) = none<Episode>(offset)
        override suspend fun setEpisodeSaved(uri: Uri, saved: Boolean) = no(uri)
    }

    override val users: Users = object : Users {
        override suspend fun me(): Account = no("account")
        override suspend fun profile(userUri: Uri): UserProfile = no(userUri)
        override suspend fun followers(userUri: Uri): List<UserRef> = emptyList()
        override suspend fun following(userUri: Uri): List<UserRef> = emptyList()
        override suspend fun top(range: TimeRange, limit: Int): TopContent = no("top")
        override suspend fun follow(userUris: List<Uri>) = no("follow")
        override suspend fun unfollow(userUris: List<Uri>) = no("follow")
        override suspend fun isFollowing(userUris: List<Uri>): List<Boolean> = userUris.map { false }
        override suspend fun isBlocked(userUri: Uri): Boolean = false
        override suspend fun blockUser(userUri: Uri) = no(userUri)
        override suspend fun unblockUser(userUri: Uri) = no(userUri)
        override suspend fun friendActivity(): List<FriendActivity> = emptyList()
        override suspend fun history(): List<HistoryEntry> = emptyList()
        override suspend fun setPrivateSession(on: Boolean) {}
    }

    override val extras: TrackExtras = object : TrackExtras {
        override suspend fun lyrics(trackUri: Uri): Lyrics? = null
        override suspend fun canvas(trackUri: Uri): Canvas? = null
        override suspend fun credits(trackUri: Uri): Credits = no(trackUri)
        override suspend fun radio(seedUri: Uri): Uri? = null
        override suspend fun setExcludedFromTaste(trackUri: Uri, excluded: Boolean) = no(trackUri)
        override suspend fun isExcludedFromTaste(trackUris: List<Uri>): List<Boolean> = trackUris.map { false }
        override suspend fun setArtistBanned(artistUri: Uri, banned: Boolean) = no(artistUri)
        override suspend fun isArtistBanned(artistUris: List<Uri>): List<Boolean> = artistUris.map { false }
    }

    override val devices: Devices = object : Devices {
        override val devices: StateFlow<List<Device>> = MutableStateFlow(emptyList())
        override suspend fun refresh() {}
        override suspend fun transfer(deviceId: String, play: Boolean) = no("devices")
        override suspend fun pullHere() {}
    }

    override val jam: Jam = object : Jam {
        override suspend fun availability() = JamAvailability.RESTRICTED
        override val session: StateFlow<JamSession?> = MutableStateFlow(null)
        override suspend fun refresh() {}
        override suspend fun start(): JamSession = no("jam")
        override suspend fun join(joinToken: String): JamSession = no("jam")
        override suspend fun leave() {}
        override suspend fun end() {}
        override suspend fun inviteLink(): String? = null
        override suspend fun removeMember(userUri: Uri) {}
        override suspend fun setParticipantVolumeControl(on: Boolean) {}
    }

    override val blends: Blends = object : Blends {
        override suspend fun invite(): BlendInvite = no("blend")
        override suspend fun invitation(linkOrId: String): BlendInvitation = no("blend")
        override suspend fun join(linkOrId: String): Uri = no("blend")
        override suspend fun group(playlist: Uri): BlendGroup = no("blend")
        override suspend fun story(playlist: Uri): BlendStory? = null
        override suspend fun rename(playlist: Uri, name: String) = no("blend")
        override suspend fun leave(playlist: Uri) = no("blend")
    }

    override val recents: Recents = object : Recents {
        override suspend fun recentEntries(offset: Int, limit: Int) = none<RecentEntry>(offset)
    }
}
