import { parseDot } from "@msagl/parser"
import { RendererSvg } from "@msagl/renderer-svg"
import { Graph } from "@msagl/core"

const viewer = document.getElementById("viewer")
const renderer = new RendererSvg(viewer)
const viewerData = document.getElementById("viewer-data") as HTMLTextAreaElement

const defaultData = `graph G {\n
    kspacey -- swilliams;\n
    swilliams -- kbacon;\n
    bpitt -- kbacon;\n
    hford -- lwilson;\n
    lwilson -- kbacon;\n
}\n`;

if (viewerData) {
    viewerData.value = defaultData

    const graph: Graph = parseDot(defaultData)
    renderer.setGraph(graph)

    viewerData.onchange = () => {
        const graph: Graph = parseDot(viewerData.value)
        renderer.setGraph(graph)
    }
}
