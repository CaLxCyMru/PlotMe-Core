package com.worldcretornica.plotme_core.api.event;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.api.IPlayer;

public class PlotRemoveAllowedEvent extends PlotEvent implements ICancellable, Event {

    private final IPlayer player;
    private final String removed;
    private boolean canceled;

    public PlotRemoveAllowedEvent(Plot plot, IPlayer player, String removed) {
        super(plot);
        this.player = player;
        this.removed = removed;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean cancel) {
        canceled = cancel;
    }

    public IPlayer getPlayer() {
        return player;
    }

    public String getRemovedAllowed() {
        return removed;
    }
}
