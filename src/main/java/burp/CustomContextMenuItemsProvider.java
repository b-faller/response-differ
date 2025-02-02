package burp;

import java.util.List;
import java.util.Optional;

import javax.swing.JMenuItem;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import java.awt.*;

public class CustomContextMenuItemsProvider implements ContextMenuItemsProvider {

    private final MontoyaApi api;

    public CustomContextMenuItemsProvider(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        HttpRequestResponse requestResponse = event.messageEditorRequestResponse().isPresent()
                ? event.messageEditorRequestResponse().get().requestResponse()
                : event.selectedRequestResponses().get(0);
        Optional<HttpResponse> response = Optional.ofNullable(requestResponse.response());
        if (response.isEmpty()) {
            return null;
        }

        JMenuItem menuItem = new JMenuItem("Set As Diff Base");
        menuItem.addActionListener(l -> {
            BaseResponse.baseResponse = response;
        });
        List<Component> menuItemList = List.of(menuItem);

        return menuItemList;
    }
}
