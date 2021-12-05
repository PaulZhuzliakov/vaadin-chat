package letscode.vaadin.chat;

import com.github.rjeschke.txtmark.Processor;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Route("")
@Push
public class MainView extends VerticalLayout {
    private final Storage storage;
    private Registration registration;
    private Grid<Storage.ChatMessage> grid;
    private VerticalLayout chat;
    private VerticalLayout login;
    private String user = "";

    public MainView(Storage storage) {
        this.storage = storage;
        buildLogin();
        buildChat();
    }

    private void buildLogin() {
        login = new VerticalLayout() {{
            TextField field = new TextField();
            field.setPlaceholder("Please, introduce yourself");
            add(
                    field,
                    new Button("login") {{
                        addClickListener(buttonClickEvent -> {
                           login.setVisible(false);
                           chat.setVisible(true);
                           user = field.getValue();
                           storage.addRecordJoined(user);
                        });
                        addClickShortcut(Key.ENTER);
                    }}
            );
        }};
        add(login);
    }

    private void buildChat() {
        chat = new VerticalLayout();
        add(chat);
        chat.setVisible(false);
        grid = new Grid<>();
        grid.setItems(storage.getMessages());
        grid.addColumn(new ComponentRenderer<>(msg -> new Html(renderRow(msg))))
                .setAutoWidth(true);

        TextField textField = new TextField();

        chat.add(
                new H3("Vaadin chat"),
                grid,
                new HorizontalLayout() {{
                    add(
                            textField,
                            new Button("Send message") {
                                {
                                    addClickListener(buttonClickEvent -> {
                                        storage.addRecord(user, textField.getValue());
                                        textField.clear();
                                    });
                                    addClickShortcut(Key.ENTER);
                                }
                            }
                    );
                }}
        );
    }

    public void onMessage(Storage.ChatEvent event) {
        if (getUI().isPresent()) {
            UI ui = getUI().get();
            ui.getSession().lock();
            ui.beforeClientResponse(grid, ctx -> grid.scrollToEnd());
//            ui.getPage().executeJs("$0._scrollToIndex($1)", grid, storage.size());
            ui.access(() -> grid.getDataProvider().refreshAll());
            ui.getSession().unlock();
        }
    }

    private String renderRow(Storage.ChatMessage msg) {
        if (msg.getName().isEmpty()) {
            return Processor.process(String.format("_User **%s** joined the chat_", msg.getMsg()));
        } else {
            return Processor.process(String.format("**%s**: %s", msg.getName(), msg.getMsg()));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        registration = storage.attachListener(this::onMessage);

    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        registration.remove();
    }
}
