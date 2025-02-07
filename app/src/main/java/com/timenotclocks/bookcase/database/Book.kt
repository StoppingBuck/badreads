/*
  Fenimore Love | 2021

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/


package com.timenotclocks.bookcase.database

import android.widget.Button
import androidx.room.*
import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * TODO: too dumb to figure out how many to many works with Room
 */

// TODO: Add converter to Room data class
enum class ShelfType(val shelf: String) {
    CurrentShelf("currently-reading"), ReadShelf("read"), ToReadShelf("to-read")
}

enum class SortColumn(val column: String) {
    DateAdded("dateAdded"), DateRead("dateRead"), DateStarted("dateStarted"),
    Title("title"), Author("author"), Year("year")
}



@Entity(
        tableName = "books",
        indices = arrayOf(Index(value = ["title", "author"], unique = true)),
)
data class Book(  // TODO: can I remove overloads? i'ts for the converter
        @PrimaryKey(autoGenerate = true) var bookId: Long,
        @ColumnInfo(name = "title") var title: String,
        @ColumnInfo(name = "subtitle") var subtitle: String?,
        @ColumnInfo(name = "isbn10") var isbn10: String?,
        @ColumnInfo(name = "isbn13") var isbn13: String?,
        @ColumnInfo(name = "author") var author: String?,
        @ColumnInfo(name = "authorExtras") var authorExtras: String?,
        @ColumnInfo(name = "publisher") var publisher: String?,
        @ColumnInfo(name = "year") var year: Int?,
        @ColumnInfo(name = "originalYear") var originalYear: Int?,
        @ColumnInfo(name = "numberPages") var numberPages: Int?,
        @ColumnInfo(name = "dateAdded") var dateAdded: Long?,
        @ColumnInfo(name = "dateStarted") var dateStarted: Long?,
        @ColumnInfo(name = "dateRead") var dateRead: Long?,
        // create column for every shelf added
        @ColumnInfo(name = "rating") var rating: Int?,
        @ColumnInfo(name = "shelf") var shelf: String,
        @ColumnInfo(name = "notes") var notes: String?
) {
    fun cover(size: String = "M"): String? {
        val isbn = isbn13 ?: isbn10
        if (isbn.isNullOrBlank()) {
            return null
        }
        return "https://covers.openlibrary.org/b/isbn/$isbn-$size.jpg?default=false"
    }

    fun titleString(): String {
        return subtitle?.let {
            sub -> "$title: $sub"
        } ?: title
    }
    fun authorString(): String {
        if (author != null && !authorExtras.isNullOrEmpty()) {
            return "$author, $authorExtras"
        }

        return author ?: ""
    }
    fun yearString(): String? {
        if (year != null && originalYear != null) {
            if (year == originalYear) {
                return year.toString()
            } else {
                return "$year ($originalYear)"
            }
        }

        (year ?: originalYear)?.let {
            return it.toString()
        }

        return null
    }

    fun shelfString(): String {
        return when (shelf) {
            ShelfType.ReadShelf.shelf -> {
                "Read"
            }
            ShelfType.CurrentShelf.shelf -> {
                "Reading"
            }
            ShelfType.ToReadShelf.shelf -> {
                "To Read"
            }
            else -> {
                "Unshelved"
            }
        }
    }

    fun shelve(target: ShelfType, button: Button?, bookViewModel: BookViewModel?): Boolean {
        if (shelf != target.shelf) {
            shelf = target.shelf
            when (target) {
                ShelfType.ReadShelf -> {
                    dateRead = LocalDate.now().toEpochDay()
                }
                ShelfType.CurrentShelf -> {
                    dateStarted = LocalDate.now().toEpochDay()
                }
                else -> {}
            }
            if (dateAdded == null) {
                dateAdded = LocalDate.now().toEpochDay()
            }
            button?.text = shelf

            bookViewModel?.update(this)
        }
        return true
    }
}



var csvDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

@Target(AnnotationTarget.FIELD)
annotation class KlaxonDate

// TODO: remove this
val dateConverter = object : Converter {
    var defaultLocalDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun canConvert(cls: Class<*>) = cls == LocalDate::class.java

    override fun fromJson(jv: JsonValue): LocalDate? {
        if (jv.string != null) {
            return LocalDate.parse(jv.string, defaultLocalDateFormatter)
        } else {
            return null
        }
    }

    override fun toJson(o: Any): String {
        return """ "$o" """
    }
}

fun fakeBook(
        id: Long = 0, title: String = "Dark Money",
        author: String? = "Jane Mayer",
        authorExtras: String? = "edit. Someoneelse",
        isbn13: String? = "978-0-385-53559-5",
        year: Int? = null,
        publisher: String? = null,
): Book {
    return Book(
            bookId = id,
            title = title,
            subtitle = "How Subs Change Books",
            isbn10 = "0123456789",
            isbn13 = isbn13,
            author = author,
            authorExtras = authorExtras,
            publisher = publisher,
            year = year,
            originalYear = 2010,
            numberPages = 290,
            rating = null,
            shelf = "currently-reading",
            notes = null,
            dateAdded = LocalDate.now().toEpochDay(),
            dateStarted = null,
            dateRead = null,
    )
}

fun emptyBook(
        fullTitle: String,
        author: String?,
): Book {
    var title: String = fullTitle
    var subtitle: String? = null
    if (fullTitle.contains(":")) {
        title = fullTitle.split(":").first().trim()
        subtitle = fullTitle.split(":")[1].trim()
    }
    return Book(
            bookId = 0,
            title = title,
            subtitle = subtitle,
            isbn10 = null,
            isbn13 = null,
            author = author,
            authorExtras = null,
            publisher = null,
            year = null,
            originalYear = null,
            numberPages = null,
            rating = null,
            shelf = "to-read",
            notes = null,
            dateAdded = LocalDate.now().toEpochDay(),
            dateStarted = null,
            dateRead = null,
    )
}


// Full Text Search Table
@Fts4(contentEntity = Book::class)
@Entity(tableName = "books_fts")
class BooksFts(val bookId: Long, val title: String, val subtitle: String?, val author: String?, val shelf: String)
