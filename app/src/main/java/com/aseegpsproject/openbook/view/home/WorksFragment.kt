package com.aseegpsproject.openbook.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.aseegpsproject.openbook.R
import com.aseegpsproject.openbook.api.getNetworkService
import com.aseegpsproject.openbook.data.Repository
import com.aseegpsproject.openbook.data.model.User
import com.aseegpsproject.openbook.data.model.Work
import com.aseegpsproject.openbook.database.OpenBookDatabase
import com.aseegpsproject.openbook.databinding.FragmentWorksBinding
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WorksFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WorksFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentWorksBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WorksAdapter
    private lateinit var listener: DiscoverFragment.OnWorkClickListener
    private lateinit var db: OpenBookDatabase
    private lateinit var user: User
    private lateinit var repository: Repository

    private var _works = listOf<Work>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        db = OpenBookDatabase.getInstance(requireContext())!!
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val trendingFreq = prefs.getString("trendings", "daily") ?: "daily"
        repository = Repository.getInstance(
            trendingFreq,
            db.userDao(),
            db.workDao(),
            getNetworkService()
        )
        if (context is DiscoverFragment.OnWorkClickListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnWorkClickListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWorksBinding.inflate(inflater, container, false)
        user = activity?.intent?.getSerializableExtra("USER_INFO") as User
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()
        subscribeUi(adapter)
        repository.setUserid(user.userId!!)
    }

    private fun subscribeUi(adapter: WorksAdapter) {
        repository.worksInLibrary.observe(viewLifecycleOwner) { user ->
            adapter.updateData(user.works)
        }
    }

    private fun setUpRecyclerView() {
        adapter = WorksAdapter(
            _works,
            { work -> listener.onWorkClick(work) },
            { work -> changeFavoriteWork(work) },
            requireContext()
        )
        with(binding) {
            rvBooksList.layoutManager = LinearLayoutManager(context)
            rvBooksList.adapter = adapter
        }
    }

    private fun changeFavoriteWork(work: Work) {
        lifecycleScope.launch {
            if (work.isFavorite) {
                work.isFavorite = false
                repository.deleteWorkFromLibrary(work, user.userId!!)
                Toast.makeText(context, R.string.remove_fav, Toast.LENGTH_SHORT).show()
            } else {
                work.isFavorite = true
                repository.workToLibrary(work, user.userId!!)
                Toast.makeText(context, R.string.add_fav, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BooksFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WorksFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}