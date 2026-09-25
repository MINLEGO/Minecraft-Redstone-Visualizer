package fr.minlego.redstonevisualizer.ui;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fr.minlego.redstonevisualizer.RedstoneVisualizerClient;
import fr.minlego.redstonevisualizer.VisualizerSession;
import fr.minlego.redstonevisualizer.config.RedstoneVisualizerConfig;
import fr.minlego.redstonevisualizer.core.BlockPos;
import fr.minlego.redstonevisualizer.core.Zone;
import fr.minlego.redstonevisualizer.world.WorldState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * MaLiLib screen for selecting and saving the two corners of the solo-world
 * visualizer zone.
 *
 * <p>The session owns persistence. This screen only validates user input and
 * forwards the resulting immutable corners to {@link VisualizerSession}.</p>
 */
public final class WorldSelectionScreen extends GuiBase {
    private static final int CONTENT_WIDTH = 410;
    private static final int FIELD_WIDTH = 62;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;

    private final MinecraftClient client;
    private GuiTextFieldInteger firstX;
    private GuiTextFieldInteger firstY;
    private GuiTextFieldInteger firstZ;
    private GuiTextFieldInteger secondX;
    private GuiTextFieldInteger secondY;
    private GuiTextFieldInteger secondZ;
    private ButtonGeneric toggleButton;
    private ButtonGeneric captureFirstButton;
    private ButtonGeneric captureSecondButton;
    private ButtonGeneric applyButton;
    private String message;
    private boolean messageError;

    public WorldSelectionScreen() {
        this.client = MinecraftClient.getInstance();
        setTitle("Redstone Visualizer - World");
    }

    /** Opens the world selection screen in the current client window. */
    public static void open() {
        GuiBase.openGui(new WorldSelectionScreen());
    }

    @Override
    public void initGui() {
        super.initGui();

        int left = contentLeft();
        int top = contentTop();
        int firstRow = top + 61;
        int secondRow = firstRow + 38;

        firstX = createField(left + 58, firstRow);
        firstY = createField(left + 126, firstRow);
        firstZ = createField(left + 194, firstRow);
        secondX = createField(left + 58, secondRow);
        secondY = createField(left + 126, secondRow);
        secondZ = createField(left + 194, secondRow);

        captureFirstButton = addButton(new ButtonGeneric(
                left + 266, firstRow, 138, BUTTON_HEIGHT, "Capture corner 1"),
                (button, mouseButton) -> captureCorner(1));
        captureSecondButton = addButton(new ButtonGeneric(
                left + 266, secondRow, 138, BUTTON_HEIGHT, "Capture corner 2"),
                (button, mouseButton) -> captureCorner(2));
        applyButton = addButton(new ButtonGeneric(
                left, top + 144, 124, BUTTON_HEIGHT, "Save coordinates"),
                (button, mouseButton) -> applyCoordinates());
        toggleButton = addButton(new ButtonGeneric(
                left + 134, top + 144, 100, BUTTON_HEIGHT, "OFF"),
                (button, mouseButton) -> toggle());
        addButton(new ButtonGeneric(
                left + 244, top + 144, 160, BUTTON_HEIGHT, "Settings"),
                (button, mouseButton) -> RedstoneVisualizerConfig.openScreen());
        addButton(new ButtonGeneric(
                left, top + 170, 124, BUTTON_HEIGHT, "Close"),
                (button, mouseButton) -> closeGui(false));

        loadFieldsFromState();
        updateControls();
    }

    @Override
    protected void drawContents(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        int left = contentLeft();
        int top = contentTop();
        int firstRow = top + 61;
        int secondRow = firstRow + 38;

        updateControls();

        drawString(drawContext, "State: " + stateLabel(), left, top + 22, GuiBase.COLOR_WHITE);
        drawString(drawContext, "Current dimension: " + currentDimension(), left, top + 38,
                0xFFBBBBBB);
        drawString(drawContext, "Corner 1", left, firstRow + 6, GuiBase.COLOR_WHITE);
        drawString(drawContext, "Corner 2", left, secondRow + 6, GuiBase.COLOR_WHITE);
        drawString(drawContext, "X", left + 58, firstRow - 12, 0xFFBBBBBB);
        drawString(drawContext, "Y", left + 126, firstRow - 12, 0xFFBBBBBB);
        drawString(drawContext, "Z", left + 194, firstRow - 12, 0xFFBBBBBB);

        int statusY = top + 200;
        WorldState state = currentState();
        if (state.first() == null) {
            drawString(drawContext, "Corner 1 is missing.", left, statusY, 0xFFFFAA00);
            statusY += 12;
        } else {
            drawString(drawContext, "Corner 1 dimension: " + state.first().dimension(),
                    left, statusY, 0xFFBBBBBB);
            statusY += 12;
        }
        if (state.second() == null) {
            drawString(drawContext, "Corner 2 is missing.", left, statusY, 0xFFFFAA00);
            statusY += 12;
        } else {
            drawString(drawContext, "Corner 2 dimension: " + state.second().dimension(),
                    left, statusY, 0xFFBBBBBB);
            statusY += 12;
        }

        if (state.first() != null && state.second() != null
                && !state.first().dimension().equals(state.second().dimension())) {
            drawString(drawContext, "Error: corners are in different dimensions.",
                    left, statusY, 0xFFFF5555);
            statusY += 12;
        }

        Zone zone = state.zone().orElse(null);
        if (zone != null) {
            long size = zone.size();
            int threshold = RedstoneVisualizerConfig.get().getAlertThreshold();
            drawString(drawContext, "Zone size: " + zone.width() + " x " + zone.height()
                    + " x " + zone.depth() + " = " + size + " positions", left, statusY,
                    0xFFBBBBBB);
            statusY += 12;
            if (zone.exceeds(threshold)) {
                drawString(drawContext, "Warning: zone exceeds alert threshold (" + threshold
                        + ").", left, statusY, 0xFFFFAA00);
                statusY += 12;
            } else {
                drawString(drawContext, "Alert threshold: " + threshold + " positions", left,
                        statusY, 0xFFBBBBBB);
                statusY += 12;
            }
        } else {
            drawString(drawContext, "Zone size: unavailable until both corners match.", left,
                    statusY, 0xFFBBBBBB);
            statusY += 12;
        }

        if (message != null && !message.isEmpty()) {
            drawString(drawContext, message, left, statusY + 4,
                    messageError ? 0xFFFF5555 : 0xFF55FF55);
        }
    }

    private void captureCorner(int index) {
        VisualizerSession session = session();
        if (session == null || !session.isSoloWorld() || client.world == null) {
            setMessage("A solo world is required.", true);
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            setMessage("Look at a block before capturing a corner.", true);
            return;
        }

        net.minecraft.util.math.BlockPos position = hit.getBlockPos();
        WorldState.Corner corner = new WorldState.Corner(currentDimension(),
                new BlockPos(position.getX(), position.getY(), position.getZ()));
        session.setCorner(index, corner);
        loadFieldsFromState();
        setMessage("Corner " + index + " saved.", false);
    }

    private void applyCoordinates() {
        VisualizerSession session = session();
        if (session == null || !session.isSoloWorld() || client.world == null) {
            setMessage("A solo world is required.", true);
            return;
        }

        ParsedCorner first = readCorner(firstX, firstY, firstZ, 1);
        ParsedCorner second = readCorner(secondX, secondY, secondZ, 2);
        if (first == null || second == null) {
            return;
        }

        if (first.position() != null) {
            session.setCorner(1, new WorldState.Corner(currentDimension(), first.position()));
        }
        if (second.position() != null) {
            session.setCorner(2, new WorldState.Corner(currentDimension(), second.position()));
        }
        loadFieldsFromState();
        setMessage("Coordinates saved.", false);
    }

    private ParsedCorner readCorner(GuiTextFieldInteger xField, GuiTextFieldInteger yField,
            GuiTextFieldInteger zField, int index) {
        String x = xField.getText().trim();
        String y = yField.getText().trim();
        String z = zField.getText().trim();
        if (x.isEmpty() && y.isEmpty() && z.isEmpty()) {
            return new ParsedCorner(null);
        }
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            setMessage("Corner " + index + " needs X, Y and Z.", true);
            return null;
        }
        try {
            return new ParsedCorner(new BlockPos(Integer.parseInt(x), Integer.parseInt(y),
                    Integer.parseInt(z)));
        } catch (NumberFormatException exception) {
            setMessage("Corner " + index + " has invalid coordinates.", true);
            return null;
        }
    }

    private void toggle() {
        VisualizerSession session = session();
        if (session == null || !session.isSoloWorld()) {
            setMessage("A solo world is required.", true);
            return;
        }
        session.toggle();
        updateControls();
        setMessage("State changed.", false);
    }

    private void loadFieldsFromState() {
        WorldState state = currentState();
        setFields(firstX, firstY, firstZ, state.first());
        setFields(secondX, secondY, secondZ, state.second());
    }

    private static void setFields(GuiTextFieldInteger xField, GuiTextFieldInteger yField,
            GuiTextFieldInteger zField, WorldState.Corner corner) {
        if (xField == null || yField == null || zField == null) {
            return;
        }
        if (corner == null) {
            xField.setText("");
            yField.setText("");
            zField.setText("");
            return;
        }
        xField.setText(Integer.toString(corner.position().x()));
        yField.setText(Integer.toString(corner.position().y()));
        zField.setText(Integer.toString(corner.position().z()));
    }

    private void updateControls() {
        VisualizerSession session = session();
        boolean available = session != null && session.isSoloWorld();
        if (toggleButton != null) {
            toggleButton.setDisplayString(available && session.state().enabled() ? "ON" : "OFF");
            toggleButton.setEnabled(available);
        }
        if (captureFirstButton != null) {
            captureFirstButton.setEnabled(available);
        }
        if (captureSecondButton != null) {
            captureSecondButton.setEnabled(available);
        }
        if (applyButton != null) {
            applyButton.setEnabled(available);
        }
    }

    private GuiTextFieldInteger createField(int x, int y) {
        GuiTextFieldInteger field = new GuiTextFieldInteger(x, y, FIELD_WIDTH, FIELD_HEIGHT,
                textRenderer);
        field.setMaxLength(11);
        addTextField(field, ignored -> true);
        return field;
    }

    private VisualizerSession session() {
        return RedstoneVisualizerClient.session;
    }

    private WorldState currentState() {
        VisualizerSession session = session();
        return session == null ? WorldState.EMPTY : session.state();
    }

    private String stateLabel() {
        VisualizerSession session = session();
        if (session == null || !session.isSoloWorld()) {
            return "OFF (solo world required)";
        }
        return session.state().enabled() ? "ON" : "OFF";
    }

    private String currentDimension() {
        if (client.world == null) {
            return "none";
        }
        return client.world.getRegistryKey().getValue().toString();
    }

    private int contentLeft() {
        return Math.max(10, (getScreenWidth() - CONTENT_WIDTH) / 2);
    }

    private int contentTop() {
        return Math.max(18, (getScreenHeight() - 285) / 2);
    }

    private void setMessage(String value, boolean error) {
        message = value;
        messageError = error;
    }

    private record ParsedCorner(BlockPos position) {
    }
}
