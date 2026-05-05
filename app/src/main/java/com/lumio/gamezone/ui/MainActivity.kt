package com.lumio.gamezone.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lumio.gamezone.R
import com.lumio.gamezone.games.*

data class GameItem(
    val id: String,
    val name: String,
    val icon: String,
    val type: String,
    val players: String,
    val featured: Boolean = false,
    val activityClass: Class<*>
)

class MainActivity : AppCompatActivity() {

    private val games = listOf(
        GameItem("chess",       "Chess",          "♟",  "Strategy",   "2 Players",   true,  ChessActivity::class.java),
        GameItem("tictactoe",   "Tic Tac Toe",    "✕",  "Classic",    "2 Players",   false, TicTacToeActivity::class.java),
        GameItem("ludo",        "Ludo",           "🎲", "Board Game", "2–4 Players", false, LudoActivity::class.java),
        GameItem("airhockey",   "Air Hockey",     "🏒", "Arcade",     "2 Players",   false, AirHockeyActivity::class.java),
        GameItem("eightball",   "8 Ball Pool",    "🎱", "Billiards",  "2 Players",   false, EightBallActivity::class.java),
        GameItem("carrom",      "Carrom",         "⚪", "Board Game", "2–4 Players", false, CarromActivity::class.java),
        GameItem("watersort",   "Water Sort",     "🧪", "Puzzle",     "Solo",        false, WaterSortActivity::class.java),
        GameItem("blockpuzzle", "Block Puzzle",   "🟦", "Puzzle",     "Solo",        false, BlockPuzzleActivity::class.java),
        GameItem("hexafall",    "Hexa Fall",      "⬡",  "Arcade",     "Solo",        false, HexaFallActivity::class.java),
        GameItem("tiledom",     "Tile Dom",       "🀄", "Puzzle",     "Solo",        false, TileDomActivity::class.java),
        GameItem("dicedom",     "Dice Dom",       "🎰", "Strategy",   "1–2 Players", false, DiceDomActivity::class.java),
        GameItem("crazyknife",  "Crazy Knife",    "🔪", "Arcade",     "Solo",        false, CrazyKnifeActivity::class.java)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rv = findViewById<RecyclerView>(R.id.rvGames)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = GameAdapter(games) { game ->
            startActivity(Intent(this, game.activityClass))
        }
    }
}

class GameAdapter(
    private val items: List<GameItem>,
    private val onClick: (GameItem) -> Unit
) : RecyclerView.Adapter<GameAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardGame)
        val tvIcon: TextView = view.findViewById(R.id.tvGameIcon)
        val tvName: TextView = view.findViewById(R.id.tvGameName)
        val tvType: TextView = view.findViewById(R.id.tvGameType)
        val tvPlayers: TextView = view.findViewById(R.id.tvGamePlayers)
        val tvPlay: TextView = view.findViewById(R.id.tvPlayBtn)
        val tvFeatured: TextView = view.findViewById(R.id.tvFeatured)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val g = items[pos]
        h.tvIcon.text = g.icon
        h.tvName.text = g.name
        h.tvType.text = g.type.uppercase()
        h.tvPlayers.text = g.players
        h.tvFeatured.visibility = if (g.featured) View.VISIBLE else View.GONE
        h.tvPlay.text = "▶ PLAY"
        h.card.setOnClickListener { onClick(g) }
        if (g.featured) {
            h.card.setCardBackgroundColor(0xFF0F1435.toInt())
            h.tvPlay.setTextColor(0xFF000000.toInt())
            h.tvPlay.setBackgroundColor(0xFFFFD700.toInt())
        }
    }

    override fun getItemCount() = items.size
}
