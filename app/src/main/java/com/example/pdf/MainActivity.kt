package com.example.pdf

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager // <-- EKLENEN SATIR
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewCourses: RecyclerView
    private lateinit var courseAdapter: CourseAdapter
    private val courseList = mutableListOf<Course>()
    private val fullCourseList = mutableListOf<Course>()

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("ThemeDebug", "MainActivity - SettingsActivity'den RESULT_OK alındı, recreate çağrılıyor.")
            recreate()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun applyThemeAndColor() {
        val selectedColorThemeIndex = SharedPreferencesManager.getAppColorTheme(this)
        val savedThemeMode = SharedPreferencesManager.getTheme(this)

        val isNightMode = when (savedThemeMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }

        val themeResId = when (selectedColorThemeIndex) {
            0 -> if (isNightMode) R.style.Theme_Pdf_SereneBlue_Dark else R.style.Theme_Pdf_SereneBlue_Light
            1 -> if (isNightMode) R.style.Theme_Pdf_Red_Dark else R.style.Theme_Pdf_Red_Light
            2 -> if (isNightMode) R.style.Theme_Pdf_Green_Dark else R.style.Theme_Pdf_Green_Light
            3 -> if (isNightMode) R.style.Theme_Pdf_Purple_Dark else R.style.Theme_Pdf_Purple_Light
            4 -> if (isNightMode) R.style.Theme_Pdf_Orange_Dark else R.style.Theme_Pdf_Orange_Light
            5 -> if (isNightMode) R.style.Theme_Pdf_DeepPurple_Dark else R.style.Theme_Pdf_DeepPurple_Light
            6 -> if (isNightMode) R.style.Theme_Pdf_Indigo_Dark else R.style.Theme_Pdf_Indigo_Light
            7 -> if (isNightMode) R.style.Theme_Pdf_Cyan_Dark else R.style.Theme_Pdf_Cyan_Light
            8 -> if (isNightMode) R.style.Theme_Pdf_Pink_Dark else R.style.Theme_Pdf_Pink_Light
            9 -> if (isNightMode) R.style.Theme_Pdf_Brown_Dark else R.style.Theme_Pdf_Brown_Light
            else -> if (isNightMode) R.style.Theme_Pdf_SereneBlue_Dark else R.style.Theme_Pdf_SereneBlue_Light
        }
        setTheme(themeResId)
        Log.d("ThemeDebug", "MainActivity - Tema uygulandı: ${resources.getResourceEntryName(themeResId)}, Gece Modu: $isNightMode, Renk Teması: $selectedColorThemeIndex")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ThemeDebug", "MainActivity - onCreate çağrıldı.")
        applyThemeAndColor()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        val toolbar: MaterialToolbar = findViewById(R.id.topToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getGreetingMessage(this)

        recyclerViewCourses = findViewById(R.id.recyclerViewCourses)
        setupRecyclerView()
        loadCourses()
    }

    override fun onStart() {
        super.onStart()
        invalidateOptionsMenu()
    }

    private fun getGreetingMessage(context: Context): String {
        val name = SharedPreferencesManager.getUserName(context)
        if (name.isNullOrEmpty()) {
            return getString(R.string.app_name)
        }
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> getString(R.string.greeting_good_morning, name)
            in 12..17 -> getString(R.string.greeting_good_day, name)
            in 18..21 -> getString(R.string.greeting_good_evening, name)
            else -> getString(R.string.greeting_good_night, name)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return true
            }
        })

        val settingsItem = menu?.findItem(R.id.action_settings)
        settingsItem?.icon?.let { icon ->
            val newIcon = icon.mutate()
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
            val settingsIconColor = typedValue.data
            newIcon.setTint(settingsIconColor)
            settingsItem.icon = newIcon
        }
        return true
    }

    private fun filter(text: String?) {
        val filteredList = mutableListOf<Course>()
        if (text.isNullOrEmpty()) {
            filteredList.addAll(fullCourseList)
        } else {
            val query = text.lowercase(Locale.getDefault()).trim()
            for (course in fullCourseList) {
                if (course.title.lowercase(Locale.getDefault()).contains(query) ||
                    course.topics.any { it.lowercase(Locale.getDefault()).contains(query) }) {
                    filteredList.add(course)
                }
            }
        }
        courseAdapter.filterList(filteredList)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                UIFeedbackHelper.provideFeedback(findViewById(android.R.id.content))
                val intent = Intent(this, SettingsActivity::class.java)
                settingsLauncher.launch(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        courseAdapter = CourseAdapter(
            this,
            courseList,
            onTopicClickListener = { courseTitle, topicTitle ->
                Toast.makeText(this, getString(R.string.topic_pdf_not_found, courseTitle, topicTitle), Toast.LENGTH_SHORT).show()
            },
            onPdfClickListener = { courseTitle, topicTitle, pdfAssetName ->
                val intent = Intent(this, PdfViewActivity::class.java)
                intent.putExtra(PdfViewActivity.EXTRA_PDF_ASSET_NAME, pdfAssetName)
                intent.putExtra(PdfViewActivity.EXTRA_PDF_TITLE, "$courseTitle - $topicTitle")
                startActivity(intent)
            }
        )
        recyclerViewCourses.layoutManager = LinearLayoutManager(this)
        recyclerViewCourses.adapter = courseAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadCourses() {
        fullCourseList.clear()

        fullCourseList.add(Course(getString(R.string.course_calculus), listOf(
            getString(R.string.topic_calculus_limit), getString(R.string.topic_calculus_derivative_rules), getString(R.string.topic_calculus_derivative_apps),
            getString(R.string.topic_calculus_mean_value), getString(R.string.topic_calculus_indefinite_integral), getString(R.string.topic_calculus_definite_integral),
            getString(R.string.topic_calculus_sequences_series), getString(R.string.topic_calculus_power_series), getString(R.string.topic_calculus_multivariable),
            getString(R.string.topic_calculus_partial_derivatives), getString(R.string.topic_calculus_multiple_integrals), getString(R.string.topic_calculus_vector_analysis),
            getString(R.string.topic_calculus_fourier), getString(R.string.topic_calculus_metric_spaces)
        )))
        fullCourseList.add(Course(getString(R.string.course_complex_analysis), listOf(
            getString(R.string.topic_complex_numbers), getString(R.string.topic_complex_analytic_functions), getString(R.string.topic_complex_cauchy_riemann),
            getString(R.string.topic_complex_integration), getString(R.string.topic_complex_cauchy_integral), getString(R.string.topic_complex_taylor_laurent),
            getString(R.string.topic_complex_residue), getString(R.string.topic_complex_conformal), getString(R.string.topic_complex_dynamic_systems)
        )))
        fullCourseList.add(Course(getString(R.string.course_numerical_analysis), listOf(
            getString(R.string.topic_numerical_error_analysis), getString(R.string.topic_numerical_root_finding), getString(R.string.topic_numerical_linear_systems),
            getString(R.string.topic_numerical_iterative_methods), getString(R.string.topic_numerical_interpolation), getString(R.string.topic_numerical_least_squares),
            getString(R.string.topic_numerical_differentiation_integration), getString(R.string.topic_numerical_matrix_decomposition), getString(R.string.topic_numerical_optimization)
        )))
        fullCourseList.add(Course(getString(R.string.course_linear_algebra), listOf(
            getString(R.string.topic_linear_matrices), getString(R.string.topic_linear_systems), getString(R.string.topic_linear_vector_spaces),
            getString(R.string.topic_linear_independence), getString(R.string.topic_linear_transformations), getString(R.string.topic_linear_kernel_image),
            getString(R.string.topic_linear_eigenvalues), getString(R.string.topic_linear_diagonalization), getString(R.string.topic_linear_inner_product),
            getString(R.string.topic_linear_tensors), getString(R.string.topic_linear_numerical)
        )))
        fullCourseList.add(Course(getString(R.string.course_abstract_math), listOf(
            getString(R.string.topic_abstract_logic), getString(R.string.topic_abstract_set_theory), getString(R.string.topic_abstract_proof_methods),
            getString(R.string.topic_abstract_relations), getString(R.string.topic_abstract_functions), getString(R.string.topic_abstract_cardinality),
            getString(R.string.topic_abstract_category_theory), getString(R.string.topic_abstract_model_theory)
        )))
        fullCourseList.add(Course(getString(R.string.course_algebra), listOf(
            getString(R.string.topic_algebra_groups), getString(R.string.topic_algebra_cyclic_groups), getString(R.string.topic_algebra_lagrange),
            getString(R.string.topic_algebra_normal_subgroups), getString(R.string.topic_algebra_homomorphisms), getString(R.string.topic_algebra_rings_fields),
            getString(R.string.topic_algebra_module_theory)
        )))
        fullCourseList.add(Course(getString(R.string.course_number_theory), listOf(
            getString(R.string.topic_number_divisibility), getString(R.string.topic_number_prime_numbers), getString(R.string.topic_number_modular_arithmetic),
            getString(R.string.topic_number_chinese_remainder), getString(R.string.topic_number_fermat_euler),
            getString(R.string.topic_number_diophantine), getString(R.string.topic_number_cryptography), getString(R.string.topic_number_quadratic_residues),
            getString(R.string.topic_number_analytic_number_theory)
        )))
        fullCourseList.add(Course(getString(R.string.course_differential_equations), listOf(
            getString(R.string.topic_diffeq_first_order), getString(R.string.topic_diffeq_higher_order), getString(R.string.topic_diffeq_undetermined_coefficients),
            getString(R.string.topic_diffeq_variation_parameters), getString(R.string.topic_diffeq_laplace), getString(R.string.topic_diffeq_initial_value),
            getString(R.string.topic_diffeq_systems), getString(R.string.topic_diffeq_chaotic_systems), getString(R.string.topic_diffeq_numerical_solutions)
        )))
        fullCourseList.add(Course(getString(R.string.course_pde), listOf(
            getString(R.string.topic_pde_concepts), getString(R.string.topic_pde_fourier), getString(R.string.topic_pde_separation_variables),
            getString(R.string.topic_pde_heat_equation), getString(R.string.topic_pde_wave_equation), getString(R.string.topic_pde_laplace_equation),
            getString(R.string.topic_pde_green_functions), getString(R.string.topic_pde_finite_element)
        )))
        fullCourseList.add(Course(getString(R.string.course_analytic_geometry), listOf(
            getString(R.string.topic_analytic_geometry_coordinates), getString(R.string.topic_analytic_geometry_line_plane), getString(R.string.topic_analytic_geometry_conic_sections),
            getString(R.string.topic_analytic_geometry_quadric_surfaces), getString(R.string.topic_analytic_geometry_transformations), getString(R.string.topic_analytic_geometry_projective),
            getString(R.string.topic_analytic_geometry_vector_fields)
        )))
        fullCourseList.add(Course(getString(R.string.course_differential_geometry), listOf(
            getString(R.string.topic_diffgeo_curves), getString(R.string.topic_diffgeo_frenet_serret), getString(R.string.topic_diffgeo_surfaces),
            getString(R.string.topic_diffgeo_fundamental_forms), getString(R.string.topic_diffgeo_gaussian_curvature), getString(R.string.topic_diffgeo_gauss_bonnet),
            getString(R.string.topic_diffgeo_riemann_geometry), getString(R.string.topic_diffgeo_manifolds)
        )))
        fullCourseList.add(Course(getString(R.string.course_topology), listOf(
            getString(R.string.topic_topology_spaces), getString(R.string.topic_topology_metric_spaces), getString(R.string.topic_topology_continuity),
            getString(R.string.topic_topology_connectedness), getString(R.string.topic_topology_compactness), getString(R.string.topic_topology_fundamental_group),
            getString(R.string.topic_topology_knot_theory), getString(R.string.topic_topology_homology_cohomology)
        )))
        fullCourseList.add(Course(getString(R.string.course_probability), listOf(
            getString(R.string.topic_probability_counting), getString(R.string.topic_probability_conditional), getString(R.string.topic_probability_random_variables),
            getString(R.string.topic_probability_discrete_distributions), getString(R.string.topic_probability_continuous_distributions), getString(R.string.topic_probability_expected_value),
            getString(R.string.topic_probability_limit_theorems), getString(R.string.topic_probability_markov_chains), getString(R.string.topic_probability_statistical_inference)
        )))
        fullCourseList.add(Course(getString(R.string.course_functional_analysis), listOf(
            getString(R.string.topic_functional_normed_spaces), getString(R.string.topic_functional_hilbert_spaces), getString(R.string.topic_functional_linear_operators),
            getString(R.string.topic_functional_spectral_theory), getString(R.string.topic_functional_weak_topologies), getString(R.string.topic_functional_applications)
        )))

        courseList.clear()
        courseList.addAll(fullCourseList)
        courseAdapter.notifyDataSetChanged()
    }
}