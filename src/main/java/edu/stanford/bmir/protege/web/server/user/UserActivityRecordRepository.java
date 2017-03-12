package edu.stanford.bmir.protege.web.server.user;

import edu.stanford.bmir.protege.web.server.persistence.Repository;
import edu.stanford.bmir.protege.web.server.project.RecentProjectRecord;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static edu.stanford.bmir.protege.web.server.user.UserActivityRecord.*;
import static java.util.stream.Collectors.toList;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 12 Mar 2017
 */
public class UserActivityRecordRepository implements Repository {

    public static final int MAX_SIZE = 100;

    private final Datastore datastore;

    public UserActivityRecordRepository(Datastore datastore) {
        this.datastore = datastore;
    }


    @Override
    public void ensureIndexes() {
        datastore.ensureIndexes(UserActivityRecord.class);
    }

    public void save(UserActivityRecord record) {
        datastore.save(record);
    }

    public Optional<UserActivityRecord> findByUserId(UserId userId) {
        UserActivityRecord record = datastore.get(UserActivityRecord.class, userId);
        return Optional.ofNullable(record);
    }

    public void setLastLogin(@Nonnull UserId userId, long lastLogin) {
        Query<UserActivityRecord> query = queryByUserId(userId);
        UpdateOperations<UserActivityRecord> operations = datastore.createUpdateOperations(UserActivityRecord.class)
                                                                   .set(LAST_LOGIN, lastLogin);
        datastore.update(query, operations);
    }

    public void setLastLogout(@Nonnull UserId userId, long lastLogout) {
        Query<UserActivityRecord> query = queryByUserId(userId);
        UpdateOperations<UserActivityRecord> operations = datastore.createUpdateOperations(UserActivityRecord.class)
                                                                   .set(LAST_LOGOUT, lastLogout);
        datastore.update(query, operations);
    }

    public void addRecentProject(@Nonnull UserId userId, @Nonnull ProjectId projectId, long timestamp) {
        findByUserId(userId)
                .ifPresent(record -> {
                    List<RecentProjectRecord> recentProjects = record.getRecentProjects().stream()
                                                                     .filter(recentProject -> !recentProject.getProjectId()
                                                                                                     .equals(projectId))
                                                                     .sorted()
                                                                     .collect(toList());
                    recentProjects.add(0, new RecentProjectRecord(projectId, timestamp));
                    UserActivityRecord replacement = new UserActivityRecord(
                            record.getUserId(),
                            record.getLastLogin(),
                            record.getLastLogout(),
                            recentProjects
                    );
                    save(replacement);
                });
    }


    private Query<UserActivityRecord> queryByUserId(@Nonnull UserId userId) {
        return datastore.createQuery(UserActivityRecord.class)
                        .field(USER_ID).equal(userId);
    }
}
