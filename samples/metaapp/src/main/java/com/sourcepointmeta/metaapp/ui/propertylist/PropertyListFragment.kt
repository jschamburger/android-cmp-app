package com.sourcepointmeta.metaapp.ui.propertylist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sourcepointmeta.metaapp.BuildConfig
import com.sourcepointmeta.metaapp.R
import com.sourcepointmeta.metaapp.core.addFragment
import com.sourcepointmeta.metaapp.data.localdatasource.Property
import com.sourcepointmeta.metaapp.ui.BaseState.* // ktlint-disable
import com.sourcepointmeta.metaapp.ui.component.PropertyAdapter
import com.sourcepointmeta.metaapp.ui.component.SwipeToDeleteCallback
import com.sourcepointmeta.metaapp.ui.component.toPropertyDTO
import com.sourcepointmeta.metaapp.ui.demo.DemoActivity
import com.sourcepointmeta.metaapp.ui.property.AddUpdatePropertyFragment
import kotlinx.android.synthetic.main.fragment_property_list.*// ktlint-disable
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.qualifier.named

class PropertyListFragment : Fragment() {

    private val viewModel: PropertyListViewModel by viewModel()
    private val clearDb: Boolean by inject(qualifier = named("clear_db"))

    private val errorColor: Int by lazy {
        TypedValue().apply { requireContext().theme.resolveAttribute(R.attr.colorErrorResponse, this, true) }
            .data
    }

    private val adapter by lazy { PropertyAdapter() }
    private val itemTouchHelper by lazy { ItemTouchHelper(swipeToDeleteCallback) }
    private val swipeToDeleteCallback: SwipeToDeleteCallback by lazy {
        SwipeToDeleteCallback(requireContext()) { showDeleteDialog(it, adapter) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_property_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (clearDb) {
            viewModel.clearDB()
        }

        tool_bar.title = "${getString(R.string.app_name)} - ${BuildConfig.VERSION_NAME}"

        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is StatePropertyList -> successState(it)
                is StateError -> errorState(it)
                is StateProperty -> updateProperty(it)
                is StateLoading -> savingProperty(it.propertyName, it.loading)
                is StateVersion -> showVersionPopup(it.version)
            }
        }
        property_list.layoutManager = GridLayoutManager(context, 1)
        property_list.adapter = adapter
        fab.setOnClickListener {
            (activity as? AppCompatActivity)?.addFragment(R.id.container, AddUpdatePropertyFragment())
        }
        (activity as? AppCompatActivity)?.supportFragmentManager?.addOnBackStackChangedListener {
            viewModel.fetchPropertyList()
        }
        adapter.itemClickListener = {
            (activity as? AppCompatActivity)?.addFragment(
                R.id.container,
                AddUpdatePropertyFragment.instance(it.propertyName)
            )
        }
        adapter.propertyChangedListener = { viewModel.updateProperty(it) }
        adapter.demoProperty = { runDemo(it) }
        itemTouchHelper.attachToRecyclerView(property_list)
        viewModel.fetchLatestVersion()
    }

    private fun updateProperty(state: StateProperty) {
        adapter.updateProperty(state.property.toPropertyDTO())
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchPropertyList()
    }

    private fun successState(it: StatePropertyList) {
        it.propertyList
            .map { p -> p.toPropertyDTO() }
            .let { adapter.addItems(it) }
    }

    private fun errorState(it: StateError) {
    }

    private fun showDeleteDialog(position: Int, adapter: PropertyAdapter) {
        val propertyName = adapter.getPropertyNameByPosition(position)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Property $propertyName")
            .setPositiveButton("Confirm") { _, _ ->
                adapter.notifyItemChanged(position)
                viewModel.deleteProperty(propertyName)
            }
            .setNegativeButton("Cancel") { _, _ -> adapter.notifyItemChanged(position) }
            .show()
    }

    private fun showVersionPopup(version: String) {
        tool_bar.setTitleTextColor(errorColor)
        tool_bar.title = "${tool_bar.title} -> $version"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Metaapp version ${BuildConfig.VERSION_NAME} out of date, new version $version is available.")
            .setPositiveButton("Update it") { _, _ ->
                val uriBuilder = Uri.parse("https://play.google.com/store/apps/details")
                    .buildUpon()
                    .appendQueryParameter("id", "com.sourcepointmeta.metaapp")
                    .appendQueryParameter("launch", "true")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = uriBuilder.build()
                    setPackage("com.android.vending")
                }
                startActivity(intent)
            }
            .setNegativeButton("Continue") { _, _ -> }
            .show()
    }

    private fun savingProperty(propertyName: String, showLoading: Boolean) {
        adapter.savingProperty(propertyName, showLoading)
    }

    private fun runDemo(property: Property) {
//        if (property.hasActiveCampaign()) {
        val bundle = Bundle()
        bundle.putString("property_name", property.propertyName)
        val i = Intent(activity, DemoActivity::class.java)
        i.putExtras(bundle)
        startActivity(i)
//        } else {
//            MaterialAlertDialogBuilder(requireContext())
//                .setTitle("You need at least one active campaign to run the demo.")
//                .setPositiveButton("OK", null)
//                .show()
//        }
    }
}
