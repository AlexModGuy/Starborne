package net.starborne.client.render.blocksystem.chunk;

import net.starborne.client.render.blocksystem.BlockSystemRenderer;
import net.starborne.server.blocksystem.BlockSystem;

public interface RenderChunkFactory {
    BlockSystemRenderChunk create(BlockSystem blockSystem, BlockSystemRenderer renderer);
}
