package br.com.devplugins.staging;

import java.util.List;

public interface StagedCommandRepository {
    void save(StagedCommand command);

    void delete(StagedCommand command);

    List<StagedCommand> loadAll();
}
