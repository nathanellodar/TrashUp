package com.bangkit.trashup.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bangkit.trashup.databinding.FragmentHomeBinding
import com.bangkit.trashup.ui.ArticlesAdapter
import com.bangkit.trashup.ui.ViewModelFactory
import com.bangkit.trashup.data.Result
import com.bangkit.trashup.data.remote.request.ViewRequest
import com.bangkit.trashup.data.remote.response.DatasItem
import com.bangkit.trashup.data.remote.retrofit.ApiService
import com.bangkit.trashup.data.remote.retrofit.RetrofitClient
import com.bangkit.trashup.ui.detail.DetailArticlesActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(), ArticlesAdapter.OnItemClickCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var storyAdapter: ArticlesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        // Inisialisasi ViewModel
        homeViewModel = ViewModelProvider(
            this,
            ViewModelFactory.getInstance(requireContext())
        )[HomeViewModel::class.java]

        // Inisialisasi Adapter
        storyAdapter = ArticlesAdapter(this)

        // Konfigurasi RecyclerView
        binding.rvPopularTutor.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = storyAdapter
        }

        // Observasi data dari ViewModel
        homeViewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }

                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    result.data.let { articles ->
                        val sortedArticles = articles.sortedByDescending { it.totalView }

                        val topArticles = sortedArticles.take(5)

                        storyAdapter.submitList(topArticles)
                    }
                }

                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${result.error}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        homeViewModel.updateViewResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }

                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), result.data, Toast.LENGTH_SHORT).show() // Pesan sukses
                }

                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        homeViewModel.getStories()

//        // Tombol untuk Analyze
//        binding.fabAnalyze.setOnClickListener {
//            val intent = Intent(requireContext(), UploadFragment::class.java)
//            startActivity(intent)
//        }
//
//        binding.fabToMaps.setOnClickListener {
//            val intent = Intent(requireContext(), MapsFragment::class.java)
//            startActivity(intent)
//        }

        // Konfigurasi WebView untuk menampilkan peta
        configureMapWebView()
    }

//    private fun getData() {
//        val adapter = ArticlesAdapter(this)
//        binding.rvPopularTutor.adapter = adapter.withLoadStateFooter(
//            footer = LoadingStateAdapter{
//                adapter.retry()
//            }
//        )
//        homeViewModel.stories.observe(viewLifecycleOwner) {
//            adapter.submitData(lifecycle, it)
//        }
//    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureMapWebView() {
        val webView: WebView = binding.webViewMaps
        val webSettings: WebSettings = webView.settings

        // Aktifkan JavaScript dan penyimpanan DOM
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // Pastikan link tetap terbuka di WebView
        webView.webViewClient = WebViewClient()

        // Atur WebChromeClient untuk menangani permintaan izin geolokasi
        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)  // Setiap permintaan izin untuk geolokasi akan langsung diizinkan
            }
        }

        // Embed HTML untuk peta
        val mapHtml = """
        <html>
        <body>
        <iframe 
            src="https://www.google.com/maps/d/u/0/embed?mid=1VR5wH1YBy3y6mSYNWwVCSD0HllmMie9n"
            width="100%"
            height="100%"
            style="border:0;"
            allowfullscreen=""
            loading="lazy">
        </iframe>
        </body>
        </html>
    """.trimIndent()

        // Load HTML ke WebView
        webView.loadData(mapHtml, "text/html", "utf-8")
    }


//    private fun updateArticleView(id: Int, wasteGroup: String) {
//        val viewRequest = ViewRequest(id = id, wasteGroup = wasteGroup)
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val retrofit = RetrofitClient.create(requireContext())
//                val apiService = retrofit.create(ApiService::class.java)
//                val response = apiService.updateArticleView(viewRequest)
//
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful) {
//                        Toast.makeText(requireContext(), "View updated successfully!", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(
//                            requireContext(),
//                            "Failed to update view: ${response.code()} ${response.message()}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    override fun onItemClick(story: DatasItem) {
        story.id?.let {
            story.wasteGroup?.let { wasteGroup ->
                homeViewModel.updateArticleView(it, wasteGroup)
            }
        }

        val intent = Intent(requireContext(), DetailArticlesActivity::class.java)
        intent.putExtra(DetailArticlesActivity.EXTRA_ARTICLES, story)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
