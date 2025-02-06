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
        HttpRequestResponse requestResponse = null;
        
        // Get HTTP request/response from editor if available
        if (event.messageEditorRequestResponse().isPresent()) {
            requestResponse = event.messageEditorRequestResponse().get().requestResponse();
        }
        
        // Fall back to selected requests if editor is empty
        if (requestResponse == null && !event.selectedRequestResponses().isEmpty()) {
            requestResponse = event.selectedRequestResponses().get(0);
        }
        
        // Return null if no request/response is available
        if (requestResponse == null) {
            return null;
        }
        
        // Get the response and return null if not present
        HttpResponse response = requestResponse.response();
        if (response == null) {
            return null;
        }
        
        // Create menu item
        JMenuItem menuItem = new JMenuItem("Set As Diff Base");
        menuItem.addActionListener(l -> BaseResponse.setBaseResponse(Optional.of(response)));
        
        return List.of(menuItem);
    }
}
