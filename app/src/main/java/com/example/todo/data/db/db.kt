package com.example.todo.data.db

import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.withTransaction
import com.example.todo.domain.Event
import com.example.todo.domain.CalendarRepository
import com.example.todo.domain.FilSorter
import com.example.todo.domain.FilSorterRepository
import com.example.todo.domain.Tag
import com.example.todo.domain.TagRepository
import com.example.todo.domain.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.onekash.icaldav.client.CalDavClient
import org.onekash.icaldav.model.ICalCalendar
import org.onekash.icaldav.model.ParseResult
import org.onekash.icaldav.parser.ICalParser
import java.io.IOException
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.collections.map

@Dao
interface EventDao {
    @Upsert
    suspend fun upsertEvent(item: EventEntity): Long

    @Upsert
    suspend fun upsertEvents(items: List<EventEntity>): Unit

    @Transaction
    @Query("SELECT * from EventEntity")
    fun getAllEvents(): Flow<List<FullEventEntity>>

    @Query("DELETE from EventTagCrossRef where eventId = :eventId")
    suspend fun deleteTagsForEvent(eventId: Long)

    @Upsert
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("delete from ReminderEntity where eventId = :eventId")
    suspend fun deleteRemindersForItem(eventId: Long)

    @Insert
    suspend fun insertCrossRefs(refs: List<EventTagCrossRef>)
}

@Dao
interface TasksDao {
    @Upsert
    suspend fun upsertTodoItem(item: TaskEntity)


    @Transaction
    @Query("SELECT * from TaskEntity")
    fun getAllTodoItems(): Flow<List<FullTaskEntity>>

    @Upsert
    suspend fun upsertTag(item: TagEntity)

    @Delete
    suspend fun deleteTag(item: TagEntity)

    @Query("DELETE from ItemTagCrossRef where taskId = :taskId")
    suspend fun deleteTagsForItem(taskId: Long)

    @Query("SELECT * from TagEntity")
    fun getAllTags(): Flow<List<TagEntity>>

    @Upsert
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("delete from ReminderEntity where taskId = :taskId")
    suspend fun deleteRemindersForItem(taskId: Long)

    @Insert
    suspend fun insertCrossRefs(refs: List<ItemTagCrossRef>)

    @Query("select * from TaskEntity where id = :id")
    suspend fun getTodoItem(id: Long): FullTaskEntity
}

@Dao
interface FilSorterDao {
    @Query("SELECT * from FilSorterEntity")
    fun getAll(): Flow<List<FilSorterEntity>>

    @Upsert
    suspend fun upsert(e: FilSorterEntity)
}

class TagRepositoryImpl @Inject constructor(
    private val dao: TasksDao
) : TagRepository {
    override suspend fun saveTag(tag: Tag) = dao.upsertTag(tag.toTaskEntity())

    override fun getAllTags(): Flow<List<Tag>> =
        dao.getAllTags().map { it.map { entity -> entity.toDomain() } }
}

class CalendarRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val eventDao: EventDao,
    private val taskDao: TasksDao,
) : CalendarRepository {

    override suspend fun saveEvent(event: Event) {
        val itemEntity = event.toEventEntity()
        val reminders = event.toReminderEntities()
        val tags = event.toTagCrossRefs()

        db.withTransaction {
            eventDao.upsertEvent(itemEntity)
            eventDao.deleteRemindersForItem(itemEntity.id)
            eventDao.insertReminders(reminders)
            eventDao.deleteTagsForEvent(itemEntity.id)
            eventDao.insertCrossRefs(tags)
        }
    }

    override fun getAllEvents(): Flow<List<Event>> =
        eventDao.getAllEvents().map { it.map { entity -> entity.toDomain() } }


    override suspend fun saveTask(item: Task) {
        val itemEntity = item.toTodoItemEntity()
        val reminders = item.toReminderEntities()
        val tags = item.toTagCrossRefs()

        db.withTransaction {
            taskDao.upsertTodoItem(itemEntity)
            taskDao.deleteRemindersForItem(itemEntity.id)
            taskDao.insertReminders(reminders)
            taskDao.deleteTagsForItem(itemEntity.id)
            taskDao.insertCrossRefs(tags)
        }
    }

    override fun getAllTasks(): Flow<List<Task>> =
        taskDao.getAllTodoItems().map { it.map { entity -> entity.toDomain() } }

    override suspend fun getTask(id: Long): Task = taskDao.getTodoItem(id).toDomain()

    override suspend fun fetch() {
        withContext(Dispatchers.IO) {
            val GET_URL =
                "https://www.wise-tt.com/wtt_up_famnit/index.jsp?filterId=0;0;0;5,7,14,27,64,87,114,113,41,117,119,123;"

            class MemoryCookieJar : CookieJar {
                private val cookieStore = HashMap<String, MutableList<Cookie>>()

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookieStore[url.host] = cookies.toMutableList()
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host] ?: mutableListOf()
                }
            }

            val client = OkHttpClient.Builder()
                .cookieJar(MemoryCookieJar())
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            Log.d(null, "[1/3] Fetching page and cookies...")
            val getRequest = Request.Builder()
                .url(GET_URL)
                .get()
                .build()

            val getResponse = client.newCall(getRequest).execute()
            if (!getResponse.isSuccessful) throw IOException("Unexpected code $getResponse")

            val htmlContent = getResponse.body.string()
            val currentUrl = getResponse.request.url.toString()

            val document: Document = Jsoup.parse(htmlContent, currentUrl)

            Log.d(null, "[2/3] Extracting complete form data...")
            val form = document.selectFirst("form#form") ?: run {
                throw Exception("❌ Could not find form with id 'form'")
            }

            val actionUrl = form.absUrl("action")

            val formBodyBuilder = FormBody.Builder()
            formBodyBuilder.add("form", "form")

            for (tag in form.select("input, select, textarea")) {
                val name = tag.attr("name")
                if (name.isEmpty()) continue

                when (tag.tagName()) {
                    "input" -> {
                        val type = tag.attr("type").lowercase()
                        if ((type == "checkbox" || type == "radio") && !tag.hasAttr("checked")) {
                            continue
                        }
                        formBodyBuilder.add(name, tag.attr("value"))
                    }

                    "select" -> {
                        val selected = tag.selectFirst("option[selected]")
                        if (selected != null) {
                            formBodyBuilder.add(name, selected.attr("value"))
                        } else {
                            val first = tag.selectFirst("option")
                            formBodyBuilder.add(name, first?.attr("value") ?: "")
                        }
                    }

                    "textarea" -> {
                        formBodyBuilder.add(name, tag.text())
                    }
                }
            }

            val icalButton =
                document.select("a").firstOrNull { it.text().contains("iCal-vse") }
            val onclickText = icalButton?.attr("onclick") ?: ""

            val match = Regex("""\{'([^']+)':'[^']+'\}""").find(onclickText)
            if (match != null) {
                val buttonId = match.groupValues[1]
                formBodyBuilder.add(buttonId, buttonId)
                Log.d(null, "      -> Trigger ID: $buttonId")
            } else {
                throw Exception("❌ Could not parse the button ID. The HTML might have changed.")
            }

            Log.d(null, "[3/3] Sending POST request...")

            val postRequest = Request.Builder()
                .url(actionUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Referer", currentUrl)
                .header("Origin", "https://www.wise-tt.com")
                .post(formBodyBuilder.build())
                .build()

            val response = client.newCall(postRequest).execute()

            val finalUrl = response.request.url.toString()
            if (finalUrl != actionUrl) {
                Log.d(null, "      -> Redirected automatically to: $finalUrl")
            } else {
                Log.d(null, "      -> Stayed on URL: $finalUrl")
            }

            val contentType = response.header("Content-Type", "")?.lowercase() ?: ""
            Log.d(null, "      -> Server returned Content-Type: $contentType")
            val body = response.body.string()

            // 6. Save files safely to the provided Android directory
            if (contentType.contains("calendar") || contentType.contains("octet-stream")) {
                Log.d(null, body)
            } else {
                Log.d(null, "❌ Failed. The server rejected the form and returned HTML.")
                Log.d(null, body)
            }

            val res = ICalParser().parse(body)
            if (res is ParseResult.Error) {
                throw Exception("failed to parse")
            }
            check(res is ParseResult.Success<ICalCalendar>)
            val value = res.value

            eventDao.upsertEvents(value.events.map {
                val start = it.dtStart.toLocalDateTime().toEpochSecond(ZoneOffset.UTC)
                val end = it.dtEnd?.toLocalDateTime()?.toEpochSecond(ZoneOffset.UTC)
                EventEntity(
                    title = it.summary ?: "",
                    start = start,
                    end = end ?: (start + (it.duration?.seconds ?: run { throw Exception("") })),
                    allDay = it.isAllDay,
                    externalId = it.uid,
                    id = 0,
                )
            })
        }
    }
}

class FilSorterRepositoryImpl @Inject constructor(private val dao: FilSorterDao) :
    FilSorterRepository {
    override suspend fun saveFilSorter(item: FilSorter) = dao.upsert(item.toEntity())

    override fun getAllFilSorters(): Flow<List<FilSorter>> =
        dao.getAll().map { it.map { e -> e.toDomain() } }
}


@Database(
    entities = [TaskEntity::class, TagEntity::class, ReminderEntity::class, ItemTagCrossRef::class, EventEntity::class, EventTagCrossRef::class, FilSorterEntity::class],
    version = 9
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasksDao(): TasksDao

    abstract fun eventDao(): EventDao

    abstract fun filSorterDao(): FilSorterDao
}