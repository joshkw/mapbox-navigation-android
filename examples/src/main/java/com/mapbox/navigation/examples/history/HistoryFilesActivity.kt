package com.mapbox.navigation.examples.history

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mapbox.navigation.core.replay.history.ReplayHistoryDTO
import com.mapbox.navigation.examples.R
import kotlinx.android.synthetic.main.history_files_activity.*

@SuppressLint("HardwareIds")
class HistoryFilesActivity : AppCompatActivity() {

    companion object {
        var selectedHistory: ReplayHistoryDTO? = null

        const val RESULT_SUCCESS = "ACTION_HISTORY_LOAD_SUCCESS"
    }

    private var filesViewController: HistoryFilesViewController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_files_activity)
        setSupportActionBar(toolbar)

        val viewManager = LinearLayoutManager(recyclerView.context)
        val viewAdapter = HistoryFileAdapter()
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        filesViewController = HistoryFilesViewController()
        filesViewController!!.attach(this, viewAdapter) { historyDataResponse ->
            if (historyDataResponse == null) {
                Snackbar.make(recyclerView,
                    getString(R.string.history_failed_to_load_item),
                    Snackbar.LENGTH_LONG).setAction("Action", null
                ).show()
            } else {
                launchRideReplay(historyDataResponse)
            }
        }

        requestFileList()
        fab.setOnClickListener { requestFileList() }
    }

    override fun onStart() {
        super.onStart()

        // clear the selected history
        selectedHistory = null
    }

    private fun requestFileList() {
        fab.visibility = GONE
        filesViewController?.requestHistoryFiles(this) { connected ->
            if (!connected) {
                Snackbar.make(recyclerView,
                    getString(R.string.history_failed_to_load_list),
                    Snackbar.LENGTH_LONG).setAction("Action", null
                ).show()
                fab.visibility = VISIBLE
            }
        }
    }

    /**
     * Decided not to pass the large file in an Intent.
     */
    private fun launchRideReplay(replayHistoryDTO: ReplayHistoryDTO) {
        selectedHistory = replayHistoryDTO

        val returnIntent = Intent()
        returnIntent.putExtra("result", RESULT_SUCCESS)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }
}
