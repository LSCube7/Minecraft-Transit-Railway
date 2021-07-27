package mtr.gui;

import mtr.MTR;
import mtr.config.Config;
import mtr.render.MoreRenderLayers;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public interface IGui {

	int SQUARE_SIZE = 20;
	int PANEL_WIDTH = 144;
	int TEXT_HEIGHT = 8;
	int TEXT_PADDING = 6;
	int TEXT_FIELD_PADDING = 4;
	int LINE_HEIGHT = 10;

	float SMALL_OFFSET_16 = 0.05F;
	float SMALL_OFFSET = SMALL_OFFSET_16 / 16;

	int RGB_WHITE = 0xFFFFFF;
	int ARGB_WHITE = 0xFFFFFFFF;
	int ARGB_BLACK = 0xFF000000;
	int ARGB_WHITE_TRANSLUCENT = 0x7FFFFFFF;
	int ARGB_BLACK_TRANSLUCENT = 0x7F000000;
	int ARGB_LIGHT_GRAY = 0xFFAAAAAA;
	int ARGB_BACKGROUND = 0xFF121212;

	int MAX_LIGHT = 0xF000B0; // LightmapTextureManager.pack(0xFF,0xFF); doesn't work with shaders

	static String formatStationName(String name) {
		return name.replace('|', ' ');
	}

	static String textOrUntitled(String text) {
		return text.isEmpty() ? new TranslatableText("gui.mtr.untitled").getString() : text;
	}

	static String formatVerticalChinese(String text) {
		final StringBuilder textBuilder = new StringBuilder();

		for (int i = 0; i < text.length(); i++) {
			final boolean isChinese = Character.isIdeographic(text.codePointAt(i));
			if (isChinese) {
				textBuilder.append('|');
			}
			textBuilder.append(text, i, i + 1);
			if (isChinese) {
				textBuilder.append('|');
			}
		}

		String newText = textBuilder.toString();
		while (newText.contains("||")) {
			newText = newText.replace("||", "|");
		}

		return newText;
	}

	static String addToStationName(String name, String prefixCJK, String prefix, String suffixCJK, String suffix) {
		final String[] nameSplit = name.split("\\|");
		final StringBuilder newName = new StringBuilder();
		for (final String namePart : nameSplit) {
			if (namePart.codePoints().anyMatch(Character::isIdeographic)) {
				newName.append(prefixCJK).append(namePart).append(suffixCJK);
			} else {
				newName.append(prefix).append(namePart).append(suffix);
			}
			newName.append("|");
		}
		return newName.deleteCharAt(newName.length() - 1).toString();
	}

	static String mergeStations(List<String> stations) {
		final List<List<String>> combinedCJK = new ArrayList<>();
		final List<List<String>> combined = new ArrayList<>();

		for (final String station : stations) {
			final String[] stationSplit = station.split("\\|");
			final List<String> currentStationCJK = new ArrayList<>();
			final List<String> currentStation = new ArrayList<>();

			for (final String stationSplitPart : stationSplit) {
				if (stationSplitPart.codePoints().anyMatch(Character::isIdeographic)) {
					currentStationCJK.add(stationSplitPart);
				} else {
					currentStation.add(stationSplitPart);
				}
			}

			for (int i = 0; i < currentStationCJK.size(); i++) {
				if (i < combinedCJK.size()) {
					if (!combinedCJK.get(i).contains(currentStationCJK.get(i))) {
						combinedCJK.get(i).add(currentStationCJK.get(i));
					}
				} else {
					final int index = i;
					combinedCJK.add(new ArrayList<String>() {{
						add(currentStationCJK.get(index));
					}});
				}
			}

			for (int i = 0; i < currentStation.size(); i++) {
				if (i < combined.size()) {
					if (!combined.get(i).contains(currentStation.get(i))) {
						combined.get(i).add(currentStation.get(i));
					}
				} else {
					final int index = i;
					combined.add(new ArrayList<String>() {{
						add(currentStation.get(index));
					}});
				}
			}
		}

		final List<String> flattened = combinedCJK.stream().map(subList -> subList.stream().reduce((a, b) -> a + new TranslatableText("gui.mtr.separator_cjk").getString() + b).orElse("")).collect(Collectors.toList());
		flattened.addAll(combined.stream().map(subList -> subList.stream().reduce((a, b) -> a + new TranslatableText("gui.mtr.separator").getString() + b).orElse("")).collect(Collectors.toList()));
		return flattened.stream().reduce((a, b) -> a + "|" + b).orElse("");
	}

	static void drawStringWithFont(MatrixStack matrices, TextRenderer textRenderer, String text, float x, float y) {
		drawStringWithFont(matrices, textRenderer, text, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, x, y, 1, ARGB_WHITE, true, null);
	}

	static void drawStringWithFont(MatrixStack matrices, TextRenderer textRenderer, String text, HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, float x, float y, float scale, int textColor, boolean shadow, DrawingCallback drawingCallback) {
		drawStringWithFont(matrices, textRenderer, text, horizontalAlignment, verticalAlignment, horizontalAlignment, x, y, -1, -1, scale, textColor, shadow, drawingCallback);
	}

	static void drawStringWithFont(MatrixStack matrices, TextRenderer textRenderer, String text, HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, DrawingCallback drawingCallback) {
		drawStringWithFont(matrices, textRenderer, text, horizontalAlignment, verticalAlignment, horizontalAlignment, x, y, maxWidth, maxHeight, scale, textColor, shadow, drawingCallback);
	}

	static void drawStringWithFont(MatrixStack matrices, TextRenderer textRenderer, String text, HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, DrawingCallback drawingCallback) {
		final Style style = Config.useMTRFont() ? Style.EMPTY.withFont(new Identifier(MTR.MOD_ID, "mtr")) : Style.EMPTY;

		while (text.contains("||")) {
			text = text.replace("||", "|");
		}
		final String[] stringSplit = text.split("\\|");

		final List<Boolean> isCJKList = new ArrayList<>();
		final List<OrderedText> orderedTexts = new ArrayList<>();
		int totalHeight = 0, totalWidth = 0;
		for (final String stringSplitPart : stringSplit) {
			final boolean isCJK = stringSplitPart.codePoints().anyMatch(Character::isIdeographic);
			isCJKList.add(isCJK);

			final OrderedText orderedText = new LiteralText(stringSplitPart).fillStyle(style).asOrderedText();
			orderedTexts.add(orderedText);

			totalHeight += LINE_HEIGHT * (isCJK ? 2 : 1);
			final int width = textRenderer.getWidth(orderedText) * (isCJK ? 2 : 1);
			if (width > totalWidth) {
				totalWidth = width;
			}
		}

		if (maxHeight >= 0 && totalHeight / scale > maxHeight) {
			scale = totalHeight / maxHeight;
		}

		matrices.push();

		final float totalWidthScaled;
		final float scaleX;
		if (maxWidth >= 0 && totalWidth > maxWidth * scale) {
			totalWidthScaled = maxWidth * scale;
			scaleX = totalWidth / maxWidth;
		} else {
			totalWidthScaled = totalWidth;
			scaleX = scale;
		}
		matrices.scale(1 / scaleX, 1 / scale, 1 / scale);

		float offset = verticalAlignment.getOffset(y * scale, totalHeight);
		for (int i = 0; i < orderedTexts.size(); i++) {
			final boolean isCJK = isCJKList.get(i);
			final int extraScale = isCJK ? 2 : 1;
			if (isCJK) {
				matrices.push();
				matrices.scale(extraScale, extraScale, 1);
			}

			final float xOffset = horizontalAlignment.getOffset(xAlignment.getOffset(x * scaleX, totalWidth), textRenderer.getWidth(orderedTexts.get(i)) * extraScale - totalWidth);
			if (shadow) {
				textRenderer.drawWithShadow(matrices, orderedTexts.get(i), xOffset / extraScale, offset / extraScale, textColor);
			} else {
				textRenderer.draw(matrices, orderedTexts.get(i), xOffset / extraScale, offset / extraScale, textColor);
			}

			if (isCJK) {
				matrices.pop();
			}

			offset += LINE_HEIGHT * extraScale;
		}

		matrices.pop();

		if (drawingCallback != null) {
			final float x1 = xAlignment.getOffset(x, totalWidthScaled / scale);
			final float y1 = verticalAlignment.getOffset(y, totalHeight / scale);
			drawingCallback.drawingCallback(x1, y1, x1 + totalWidthScaled / scale, y1 + totalHeight / scale);
		}
	}

	static void setPositionAndWidth(AbstractButtonWidget widget, int x, int y, int widgetWidth) {
		widget.x = x;
		widget.y = y;
		widget.setWidth(widgetWidth);
	}

	static int divideColorRGB(int color, int amount) {
		final int r = ((color >> 16) & 0xFF) / amount;
		final int g = ((color >> 8) & 0xFF) / amount;
		final int b = (color & 0xFF) / amount;
		return (r << 16) + (g << 8) + b;
	}

	static void drawRectangle(VertexConsumer vertexConsumer, double x1, double y1, double x2, double y2, int color) {
		final int a = (color >> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;
		if (a == 0) {
			return;
		}
		vertexConsumer.vertex(x1, y1, 0).color(r, g, b, a).next();
		vertexConsumer.vertex(x1, y2, 0).color(r, g, b, a).next();
		vertexConsumer.vertex(x2, y2, 0).color(r, g, b, a).next();
		vertexConsumer.vertex(x2, y1, 0).color(r, g, b, a).next();
	}

	static void drawRectangle(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float x1, float y1, float x2, float y2, float z, Direction facing, int color, int light) {
		drawRectangle(matrices, vertexConsumers, x1, y1, z, x2, y2, z, facing, color, light);
	}

	static void drawRectangle(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float x1, float y1, float z1, float x2, float y2, float z2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumers, "mtr:textures/block/white.png", x1, y1, z1, x2, y2, z2, 0, 0, 1, 1, facing, color, light);
	}

	static void drawTexture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, String texture, float x, float y, float width, float height, Direction facing, int light) {
		drawTexture(matrices, vertexConsumers, texture, x, y, 0, x + width, y + height, 0, 0, 0, 1, 1, facing, -1, light);
	}

	static void drawTexture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, String texture, float x, float y, float width, float height, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumers, texture, x, y, 0, x + width, y + height, 0, u1, v1, u2, v2, facing, color, light);
	}

	static void drawTexture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, String texture, float x1, float y1, float z1, float x2, float y2, float z2, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumers, texture, x1, y2, z1, x2, y2, z2, x2, y1, z2, x1, y1, z1, u1, v1, u2, v2, facing, color, light);
	}

	static void drawTexture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, String texture, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		if (vertexConsumers == null) {
			return;
		}
		final Vec3i vec3i = facing.getVector();
		final Matrix4f matrix4f = matrices.peek().getModel();
		final Matrix3f matrix3f = matrices.peek().getNormal();
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(light == -1 ? MoreRenderLayers.getLight(new Identifier(texture)) : MoreRenderLayers.getExterior(new Identifier(texture)));
		final int a = (color >> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;
		if (a == 0) {
			return;
		}
		final int newLight = light == -1 ? MAX_LIGHT : light;
		vertexConsumer.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).texture(u1, v2).overlay(OverlayTexture.DEFAULT_UV).light(newLight).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
		vertexConsumer.vertex(matrix4f, x2, y2, z2).color(r, g, b, a).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(newLight).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
		vertexConsumer.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(newLight).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
		vertexConsumer.vertex(matrix4f, x4, y4, z4).color(r, g, b, a).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(newLight).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
	}

	BlockModelRenderer blockModelRenderer = new BlockModelRenderer(null);
	BlockModelRenderer.AmbientOcclusionCalculator aoCalculator = blockModelRenderer.new AmbientOcclusionCalculator();

	static void drawBlockFace(MatrixStack matrices, VertexConsumerProvider vertexConsumers, String texture, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, float u3, float v3, float u4, float v4,
							  Direction facing, BlockPos pos, World world) {
		final int light = WorldRenderer.getLightmapCoordinates(world, pos.offset(facing));
		final float br = world.getBrightness(facing, true);
		final Vec3i vec3i = facing.getVector();
		final Matrix4f matrix4f = matrices.peek().getModel();
		final Matrix3f matrix3f = matrices.peek().getNormal();
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(MoreRenderLayers.getSolid(new Identifier(texture)));

		final BlockState bs = world.getBlockState(pos);
		BitSet flags = new BitSet(3);
		float[] box = new float[Direction.values().length * 2];
		int[] vertexData = new int[] {
				Float.floatToIntBits(x1), Float.floatToIntBits(y1), Float.floatToIntBits(z1), 0, 0, 0, 0, 0,
				Float.floatToIntBits(x2), Float.floatToIntBits(y2), Float.floatToIntBits(z2), 0, 0, 0, 0, 0,
				Float.floatToIntBits(x3), Float.floatToIntBits(y3), Float.floatToIntBits(z3), 0, 0, 0, 0, 0,
				Float.floatToIntBits(x4), Float.floatToIntBits(y4), Float.floatToIntBits(z4), 0, 0, 0, 0, 0,
		};
		blockModelRenderer.getQuadDimensions(world, bs, pos.offset(facing), vertexData, facing, box, flags);
		aoCalculator.apply(world, bs, pos.offset(facing), facing, box, flags, true);

		vertexConsumer.vertex(matrix4f, x1, y1, z1).color(aoCalculator.brightness[0], aoCalculator.brightness[0], aoCalculator.brightness[0], 1.0F)
				.texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(aoCalculator.light[0]).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
		vertexConsumer.vertex(matrix4f, x2, y2, z2).color(aoCalculator.brightness[1], aoCalculator.brightness[1], aoCalculator.brightness[1], 1.0F)
				.texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(aoCalculator.light[1]).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
		vertexConsumer.vertex(matrix4f, x3, y3, z3).color(aoCalculator.brightness[2], aoCalculator.brightness[2], aoCalculator.brightness[2], 1.0F)
				.texture(u3, v3).overlay(OverlayTexture.DEFAULT_UV).light(aoCalculator.light[2]).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
		vertexConsumer.vertex(matrix4f, x4, y4, z4).color(aoCalculator.brightness[3], aoCalculator.brightness[3], aoCalculator.brightness[3], 1.0F)
				.texture(u4, v4).overlay(OverlayTexture.DEFAULT_UV).light(aoCalculator.light[3]).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
	}

	@FunctionalInterface
	interface DrawingCallback {
		void drawingCallback(float x1, float y1, float x2, float y2);
	}

	enum HorizontalAlignment {
		LEFT, CENTER, RIGHT;

		float getOffset(float x, float width) {
			switch (this) {
				case CENTER:
					return x - width / 2;
				case RIGHT:
					return x - width;
				default:
					return x;
			}
		}
	}

	enum VerticalAlignment {
		TOP, CENTER, BOTTOM;

		float getOffset(float y, float height) {
			switch (this) {
				case CENTER:
					return y - height / 2;
				case BOTTOM:
					return y - height;
				default:
					return y;
			}
		}
	}
}
