package com.sm3.stable

import android.app.Activity
import android.os.Bundle
import android.media.MediaPlayer
import android.net.Uri
import android.content.Intent
import android.provider.DocumentsContract
import android.view.Gravity
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import java.util.Locale

class MainActivity : Activity() {
    private val REQ_FOLDER = 101
    private var player: MediaPlayer? = null
    private var tracks = mutableListOf<Uri>()
    private var index = 0

    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var playBtn: Button
    private lateinit var nextBtn: Button
    private lateinit var prevBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(30, 36, 30, 30)
        root.setBackgroundColor(Color.rgb(16,16,20))
        root.layoutParams = LinearLayout.LayoutParams(-1, -1)

        val appTitle = TextView(this)
        appTitle.text = "SM3 Stable V1.1"
        appTitle.textSize = 26f
        appTitle.setTextColor(Color.rgb(215,181,109))
        appTitle.typeface = Typeface.DEFAULT_BOLD
        root.addView(appTitle)

        title = TextView(this)
        title.text = "폴더를 선택하면 자동 재생됩니다"
        title.textSize = 18f
        title.setTextColor(Color.WHITE)
        title.setPadding(0, 28, 0, 12)
        root.addView(title)

        status = TextView(this)
        status.text = "대기 중"
        status.textSize = 14f
        status.setTextColor(Color.LTGRAY)
        root.addView(status)

        val choose = Button(this)
        choose.text = "📂 폴더 선택"
        choose.textSize = 18f
        choose.setOnClickListener { openFolderPicker() }
        root.addView(choose, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        row.setPadding(0, 26, 0, 0)

        prevBtn = Button(this); prevBtn.text = "◀ 이전"
        playBtn = Button(this); playBtn.text = "▶/Ⅱ"
        nextBtn = Button(this); nextBtn.text = "다음 ▶"

        prevBtn.setOnClickListener { previous() }
        playBtn.setOnClickListener { toggle() }
        nextBtn.setOnClickListener { next() }

        row.addView(prevBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(playBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(nextBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(row)

        val note = TextView(this)
        note.text = "\n이번 버전 실제 기능: 폴더 선택 → 오디오 파일 목록 생성 → 첫 곡 자동 재생 → 이전/다음/재생/일시정지"
        note.setTextColor(Color.GRAY)
        note.textSize = 13f
        root.addView(note)

        setContentView(root)
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(intent, REQ_FOLDER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FOLDER && resultCode == RESULT_OK && data?.data != null) {
            val treeUri = data.data!!
            contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            loadFolder(treeUri)
        }
    }

    private fun loadFolder(treeUri: Uri) {
        tracks.clear()
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val cursor = contentResolver.query(childrenUri, arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ), null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val childId = it.getString(0)
                val name = it.getString(1) ?: ""
                val mime = it.getString(2) ?: ""
                val lower = name.lowercase(Locale.ROOT)
                if (mime.startsWith("audio/") || lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac") || lower.endsWith(".m4a") || lower.endsWith(".ogg")) {
                    tracks.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, childId))
                }
            }
        }

        tracks.sortBy { it.toString() }
        index = 0
        status.text = "불러온 곡: ${tracks.size}개"
        if (tracks.isNotEmpty()) playCurrent() else Toast.makeText(this, "오디오 파일이 없습니다", Toast.LENGTH_SHORT).show()
    }

    private fun playCurrent() {
        if (tracks.isEmpty()) return
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(this@MainActivity, tracks[index])
            setOnPreparedListener {
                it.start()
                title.text = "재생 중: ${index + 1} / ${tracks.size}"
                status.text = tracks[index].lastPathSegment ?: "playing"
            }
            setOnCompletionListener { next() }
            prepareAsync()
        }
    }

    private fun next() {
        if (tracks.isEmpty()) return
        index = (index + 1) % tracks.size
        playCurrent()
    }

    private fun previous() {
        if (tracks.isEmpty()) return
        index = if (index - 1 < 0) tracks.size - 1 else index - 1
        playCurrent()
    }

    private fun toggle() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            status.text = "일시정지"
        } else {
            p.start()
            status.text = "재생 중"
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
