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

package com.timenotclocks.bookcase.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.timenotclocks.bookcase.LOG_SEARCH
import com.timenotclocks.bookcase.R
import com.timenotclocks.bookcase.database.*


const val LOG_SHELF = "BookShelf"
const val LOG_SORT = "BookSort"

class ShelfFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private val adapter = BookListAdapter()
    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory((activity?.application as BooksApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)

        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)
        val recyclerView = root.findViewById<RecyclerView>(R.id.recyclerview)
        val sortView = root.findViewById<MaterialButton>(R.id.fragment_sort_button)
        val searchView = root.findViewById<SearchView>(R.id.fragment_search_view)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)


        sortView.setOnClickListener { showMenu(it) }

        defaultSortLibrary()?.observe(viewLifecycleOwner) { books ->
            books?.let { adapter.submitList(it) }
        }

        searchLibrary(searchView, root.findViewById<ProgressBar>(R.id.fragment_progress_bar))

        return root
    }

    private fun defaultSortLibrary(): LiveData<List<Book>>? {
        Log.i(LOG_SHELF, "This is the Shelf ${pageViewModel.getIndex()}")
        when (pageViewModel.getIndex()) {
            1 -> {
                return bookViewModel.currentShelf
            }
            2 -> {
                return bookViewModel.readShelf
            }
            3 -> {
                return bookViewModel.toReadShelf
            }
        }
        return null
    }

    private fun sortLibrary(sortColumn: SortColumn): LiveData<List<Book>>? {
        Log.i(LOG_SORT, "Sorting with $sortColumn")
        when (pageViewModel.getIndex()) {
            1 -> {
                return bookViewModel.sortShelf(ShelfType.CurrentShelf, sortColumn)
            }
            2 -> {
                return bookViewModel.sortShelf(ShelfType.ReadShelf, sortColumn)
            }
            3 -> {
                return bookViewModel.sortShelf(ShelfType.ToReadShelf, sortColumn)
            }
        }
        return null
    }

    private fun searchLibrary(searchView: SearchView?, progressBar: ProgressBar?) {

        searchView?.setOnCloseListener {
            defaultSortLibrary()?.observe(viewLifecycleOwner) { books ->
                books?.let { adapter.submitList(it) }
            }
            false
        }
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchQuery(it, progressBar) }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.length > 4) {
                    searchQuery(newText, progressBar)
                }
                return true
            }
        })
    }

    private fun searchQuery(query: String, progressBar: ProgressBar?) {
        Log.i(LOG_SEARCH, "Searching Library: $query?")
        bookViewModel.query("*$query*").observe(this, { observable ->
            progressBar?.visibility = View.GONE
            observable?.let { books -> adapter.submitList(books) }
        })
        progressBar?.visibility = View.VISIBLE
    }

    private fun showMenu(v: View) {
        // TODO: doesn't work
        val shelf = when (pageViewModel.getIndex()) {
            1 -> {
                ShelfType.CurrentShelf
            }
            2 -> {
                ShelfType.ReadShelf
            }
            3 -> {
                ShelfType.ToReadShelf
            }
            else -> {ShelfType.CurrentShelf}
        }


        val popup = context?.let { PopupMenu(it, v) }

        popup?.menuInflater?.inflate(R.menu.sort_menu, popup.menu)

        popup?.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.menu_sort_date -> {
                    sortLibrary(SortColumn.DateAdded)?.observe(viewLifecycleOwner) { books ->
                        books.let {
                            Log.i(LOG_SORT, "Date Added Book ID: ${menuItem.itemId} ${it.map { it.title }}")
                            adapter.submitList(it)
                        }
                    }
                }
                R.id.menu_sort_author -> {
                    bookViewModel.authorSort(shelf).observe(viewLifecycleOwner) { books ->
                        // sortLibrary(SortColumn.Author)?.observe(viewLifecycleOwner) { books ->
                        books.let {
                            Log.i(LOG_SORT, "Author Book ID: ${menuItem.itemId} ${it.map { it.title }}")
                            adapter.submitList(it)
                        }
                    }
                }
                R.id.menu_sort_year -> {
                    bookViewModel.yearSort(shelf).observe(viewLifecycleOwner) { books ->
                        // sortLibrary(SortColumn.Title)?.observe(viewLifecycleOwner) { books ->
                        books.let {
                            Log.i(LOG_SORT, "Year ${it.map { it.title }}")
                            adapter.submitList(it)
                        }
                    }
                }
                R.id.menu_sort_rating -> {
                    bookViewModel.ratingSort(shelf).observe(viewLifecycleOwner) { books ->
                        books.let {
                            Log.i(LOG_SORT, "Rating ${it.map { it.title }}")
                            adapter.submitList(it)
                        }
                    }
                }
                R.id.menu_sort_page_num_desc -> {
                    bookViewModel.pageNumbersSortDesc(shelf).observe(viewLifecycleOwner) { books ->
                        books.let { adapter.submitList(it) }
                    }
                }
                R.id.menu_sort_page_num_asc -> {
                    bookViewModel.pageNumbersSortAsc(shelf).observe(viewLifecycleOwner) { books ->
                        books.let { adapter.submitList(it) }
                    }
                }
            }
            adapter.notifyDataSetChanged()
            true
        }
        popup?.show()
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): ShelfFragment {
            return ShelfFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}