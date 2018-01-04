package fr.openium.auvergnewebcams.event

/**
 * Created by laura on 08/12/2017.
 */
class EventCameraDateUpdate : PublishEvents<Long>()

class EventCameraFavoris : PublishEvents<Long>()

class EventNewValueDelay : PublishEvents<Int>()

object Events {

    val eventCameraDateUpdate = EventCameraDateUpdate()
    val eventCameraFavoris = EventCameraFavoris()
    val eventNewValueDelay = EventNewValueDelay()

}