package uk.co.electronstudio.cats

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2


class CatGame : ApplicationAdapter() {
    lateinit var batch: SpriteBatch
    lateinit var background: Texture
    lateinit var cam: OrthographicCamera
    lateinit var mapRenderer: OrthogonalTiledMapRenderer
    lateinit var debugRenderer: ShapeRenderer
    lateinit var origin: Thing
    lateinit var goal: Thing
    lateinit var layer: TiledMapTileLayer

    val path = Path()

    override fun create() {
        batch = SpriteBatch()
        background = Texture("space.jpg")
        cam = setupCam(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        val pm = Pixmap(Gdx.files.internal("pawprint.png"))
        Gdx.graphics.setCursor(Gdx.graphics.newCursor(pm, 0, 0))
        pm.dispose()

        val map = TmxMapLoader().load("level1.tmx")!!

        mapRenderer = OrthogonalTiledMapRenderer(map, 1f)
        debugRenderer = ShapeRenderer()


        layer = map.layers[0] as TiledMapTileLayer
        for (x: Int in 0..layer.width - 1) {
            for (y: Int in 0..layer.height - 1) {
                val cell = layer.getCell(x, y)
                //    println("$x $y")
                if (cell != null) {
                    if (cell.tile.properties.containsKey("origin")) {
                        origin = Thing(x, y)
                    }
                    if (cell.tile.properties.containsKey("goal")) {
                        goal = Thing(x, y)
                    }
                }
            }
        }
        println("origin $origin")

        calculatePath()
    }

    class Thing(val x: Int, val y: Int)

    private fun calculatePath() {
        var xVel = 0
        var yVel = 1
        var x = origin.x
        var y = origin.y
        path.points.add(Vector2(x * 128f + 64f, y * 128f + 64f))
        while (true) {
            x += xVel
            y += yVel

            println("checking cell $x $y")

            if (x > layer.width || x < 0 || y > layer.height || y < 0) {
                path.points.add(Vector2(x * 128f + 64f, y * 128f + 64f))
                println(" breaking width ${layer.width} height ${layer.height}")
                break
            }
            val cell = layer.getCell(x, y)

            if (cell == null) {
                continue
            }
            println("cell has properties ${cell.tile.properties}")
            if (cell.tile.properties.containsKey("goal")) {
                path.points.add(Vector2(x * 128f + 64f, y * 128f + 64f))
                break //win
            }
            if (cell.tile.properties.containsKey("mirror")) {
                val angle: Int = cell.tile.properties["mirror"] as Int
                println("cell is mirror $angle")
                when (angle) {
//                    0.0 ->{}
//                        45.0->{}
//
//                    90.0 -> return
                    0 -> {
                        yVel = -yVel
                    }
                    90 -> {
                        xVel = -xVel
                    }
                    45 -> {
                        println("found 45 mirror")
                        val t = xVel
                        xVel = yVel
                        yVel = t
                    }
                    135 -> {
                        println("found 135 mirror")
                        val t = xVel
                        xVel = -yVel
                        yVel = -t
                    }


                }
                path.points.add(Vector2(x * 128f + 64f, y * 128f + 64f))
            }
            path.points.add(Vector2(x * 128f + 64f, y * 128f + 64f))

        }
    }


    override fun render() {
        doInput()
        cam.update();
        batch.setProjectionMatrix(cam.combined);
        Gdx.gl.glClearColor(0f, 0f, 0.5f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        drawBackground()
        drawSprites()
        drawLaser()
    }

    private fun drawLaser() {
        batch.begin()
        for (i: Int in 0..path.points.lastIndex - 1) {
            drawLine(path.points[i], path.points[i + 1], 15, Color.RED, cam.combined)
            drawLine(path.points[i], path.points[i + 1], 3, Color.WHITE, cam.combined)
        }
        batch.end()
    }

    private fun drawSprites() {
        batch.begin()
        mapRenderer.setView(cam)


        mapRenderer.render()
        batch.end()


    }


    fun drawLine(start: Vector2, end: Vector2, lineWidth: Int, color: Color, projectionMatrix: Matrix4) {
        Gdx.gl.glLineWidth(lineWidth.toFloat())
        debugRenderer.setProjectionMatrix(projectionMatrix)
        debugRenderer.begin(ShapeRenderer.ShapeType.Line)
        debugRenderer.setColor(color)
        debugRenderer.line(start, end)
        debugRenderer.end()
        Gdx.gl.glLineWidth(1f)
    }

    private fun drawBackground() {
        batch.begin()
        batch.draw(background, 0f, 0f)
        batch.end()
    }

    private fun doInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        }
    }

    override fun dispose() {
        batch.dispose()
        background.dispose()
    }

    val WIDTH = 1920f
    val HEIGHT = 1080f

    fun setupCam(w: Float, h: Float): OrthographicCamera {

        val m: Float = findHighestScaleFactor(w, h)


        val cam = OrthographicCamera(w / m, h / m)

        cam.translate((WIDTH / 2), (HEIGHT / 2))

        cam.update()

        return cam

    }

    override fun resize(width: Int, height: Int) {
        cam = setupCam(width.toFloat(), height.toFloat())
    }

    fun findHighestScaleFactor(width: Float, height: Float): Float {

        val w = width / WIDTH
        val h = height / HEIGHT

        return if (w < h) w else h
    }

    fun Float.roundDown(): Float {
        return this.toInt().toFloat()
    }
}
