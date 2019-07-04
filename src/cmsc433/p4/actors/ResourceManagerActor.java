package cmsc433.p4.actors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

import cmsc433.p4.enums.*;
import cmsc433.p4.messages.*;
import cmsc433.p4.util.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class ResourceManagerActor extends UntypedActor {

	//Contains info on user and access level associated with given resource.
	private class UserAccess {

		private ActorRef user;
		private AccessType access;

		private UserAccess(ActorRef user, AccessType access) {
			this.user = user;
			this.access = access;
		}

		private ActorRef getUser() {
			return user;
		}

		private AccessType getAccess() {
			return access;
		}

	}

	private class DiscoverClass {
	}

	private ActorRef logger;					// Actor to send logging messages to




	/**
	 * Props structure-generator for this class.
	 * @return  Props structure
	 */
	static Props props (ActorRef logger) {
		return Props.create(ResourceManagerActor.class, logger);
	}

	/**
	 * Factory method for creating resource managers
	 * @param logger			Actor to send logging messages to
	 * @param system			Actor system in which manager will execute
	 * @return					Reference to new manager
	 */
	public static ActorRef makeResourceManager (ActorRef logger, ActorSystem system) {
		ActorRef newManager = system.actorOf(props(logger));
		return newManager;
	}

	/**
	 * Sends a message to the Logger Actor
	 * @param msg The message to be sent to the logger
	 */
	public void log (LogMsg msg) {
		logger.tell(msg, getSelf());
	}

	/**
	 * Constructor
	 *
	 * @param logger			Actor to send logging messages to
	 */
	private ResourceManagerActor(ActorRef logger) {
		super();
		this.logger = logger;
	}


	private ArrayList<ActorRef> ManagerList = new ArrayList<>();
	private ArrayList<ActorRef> LocalUserList = new ArrayList<>();
	private ArrayList<Resource> ResourceList = new ArrayList<>();
	private ArrayList<ActorRef> OtherResource = new ArrayList<>();
	private ArrayList<String> AccessList_resource = new ArrayList<>();
	private ArrayList<ManagementRequestMsg> Pending = new ArrayList<>();
	private ArrayList<Resource> BlockingList = new ArrayList<>();
	private LinkedList<AccessRequestMsg> AccessQueue = new LinkedList<>();
	private Map<String, ActorRef> knownRemote = new HashMap<String, ActorRef>();
	private Map<String, Resource> localResource = new HashMap<String, Resource>();
	private HashSet<ActorRef> allManagers = new HashSet<ActorRef>();
	private HashSet<ActorRef> localUsers = new HashSet<ActorRef>();
	private List<AccessRequestMsg> accessQueue = new LinkedList<AccessRequestMsg>();
	private Map<String, List<UserAccess>> resourceAccess = new HashMap<String, List<UserAccess>>();
	private Map<String, List<ManagementRequestMsg>> pendingDisable = new HashMap<String, List<ManagementRequestMsg>>();



	@Override
	public void onReceive(Object o) throws Exception {
		if (o instanceof AddRemoteManagersRequestMsg) {
			ActorRef sender = getSender();
			ArrayList<ActorRef> Manager = new ArrayList<>(((AddRemoteManagersRequestMsg) o).getManagerList());
			for (ActorRef actor : Manager) {
				if (!actor.equals(getSelf())) {
					ManagerList.add(actor);        //other managers
				}
			}
			AddRemoteManagersResponseMsg msg1 = new AddRemoteManagersResponseMsg(((AddRemoteManagersRequestMsg) o));
			sender.tell(msg1, getSelf());

		}
		else if (o instanceof AddInitialLocalResourcesRequestMsg) {
			ActorRef sender = getSender();
			AddInitialLocalResourcesRequestMsg msg = (AddInitialLocalResourcesRequestMsg) o;

			ArrayList<Resource> list = new ArrayList<Resource>(msg.getLocalResources());

			for (Resource resource : list) {
				resource.enable();
				String name = resource.getName();
				localResource.put(name, resource);
				logger.tell(LogMsg.makeLocalResourceCreatedLogMsg(getSelf(), name), getSelf());
				logger.tell(LogMsg.makeResourceStatusChangedLogMsg(getSelf(), name, resource.getStatus()), getSelf());
			}

			AddInitialLocalResourcesResponseMsg response = new AddInitialLocalResourcesResponseMsg(msg);
			sender.tell(response, getSelf());

		}
		else if (o instanceof AddLocalUsersRequestMsg) {
			ActorRef sender = getSender();

			LocalUserList = new ArrayList<>(((AddLocalUsersRequestMsg) o).getLocalUsers());
			AddLocalUsersResponseMsg msg1 = new AddLocalUsersResponseMsg(((AddLocalUsersRequestMsg) o));
			sender.tell(msg1, getSelf());

		}


		else if (o instanceof AccessReleaseMsg) {

		}
		else if (o instanceof AccessRequestMsg)
			{
				AccessRequest access = ((AccessRequestMsg) o).getAccessRequest();
				ActorRef sender = ((AccessRequestMsg) o).getReplyTo();

				ResourceStatus status = localResource.get(access.getResourceName()).getStatus();
				if (status == ResourceStatus.DISABLED || pendingDisable.containsKey(access.getResourceName())) {
					AccessRequestDenialReason whyTho = AccessRequestDenialReason.RESOURCE_DISABLED;
					AccessRequestDeniedMsg denied = new AccessRequestDeniedMsg(access, whyTho);
					sender.tell(denied, getSelf());
					logger.tell(LogMsg.makeAccessRequestDeniedLogMsg(sender, getSelf(), access, whyTho), getSelf());
					return;
				}

				if (!resourceAccess.containsKey(access.getResourceName())) {
					resourceAccess.put(access.getResourceName(), new ArrayList<UserAccess>());
				}
				List<UserAccess> list = resourceAccess.get(access.getResourceName());
				boolean AccessApproved = true;

				for (UserAccess user : list) {
					AccessType type = user.getAccess();
					ActorRef curr = user.getUser();

					if (type == AccessType.EXCLUSIVE_WRITE && !curr.equals(sender)) {
						AccessApproved = false;
						break;
					} else if (type == AccessType.CONCURRENT_READ && !curr.equals(sender)) {
						if (access.getType() == AccessRequestType.EXCLUSIVE_WRITE_BLOCKING || access.getType() == AccessRequestType.EXCLUSIVE_WRITE_NONBLOCKING) {
							AccessApproved = false;
							break;
						}
					}
				}
				if (AccessApproved) {
					UserAccess newAccess = new UserAccess(null,null);
					if (access.getType() == AccessRequestType.CONCURRENT_READ_BLOCKING || access.getType() == AccessRequestType.CONCURRENT_READ_NONBLOCKING) {
						newAccess = new UserAccess(sender, AccessType.CONCURRENT_READ);
					}
					else {
						newAccess = new UserAccess(sender, AccessType.EXCLUSIVE_WRITE);
					}
					resourceAccess.get(access.getResourceName()).add(newAccess);
					AccessRequestGrantedMsg granted = new AccessRequestGrantedMsg(access);
					sender.tell(granted, getSelf());

				} else {
					if (access.getType() == AccessRequestType.CONCURRENT_READ_BLOCKING || access.getType() == AccessRequestType.EXCLUSIVE_WRITE_BLOCKING) {
						accessQueue.add((AccessRequestMsg) o);
					}
					else {
						AccessRequestDenialReason whyTho = AccessRequestDenialReason.RESOURCE_BUSY;
						AccessRequestDeniedMsg rejected = new AccessRequestDeniedMsg(access, whyTho);
						sender.tell(rejected, getSelf());
					}
				}
			}


		else if (o instanceof ManagementRequestMsg){
			{
				ManagementRequest manageRequest = ((ManagementRequestMsg) o).getRequest();
				ActorRef replyTo = ((ManagementRequestMsg) o).getReplyTo();

				logger.tell(LogMsg.makeManagementRequestReceivedLogMsg(replyTo, getSelf(), manageRequest), getSelf());

				if (manageRequest.getType() == ManagementRequestType.DISABLE) {
					if (localResource.get(manageRequest.getResourceName()).getStatus() == ResourceStatus.ENABLED) {

						if (!resourceAccess.containsKey(manageRequest.getResourceName())) {
							resourceAccess.put(manageRequest.getResourceName(), new ArrayList<UserAccess>());
						}

						List<UserAccess> listOfAccess = resourceAccess.get(manageRequest.getResourceName());
						int sizeOfAccess = listOfAccess.size();
						for (int i=0;i<sizeOfAccess;i++) {
							if (listOfAccess.get(i).getUser().equals(replyTo)) {
								break;
							}
						}
							int size = AccessQueue.size();
							for (int i=0;i<size;i++){
								AccessRequestMsg access = AccessQueue.get(i);
								if (access.getAccessRequest().getResourceName().equals(manageRequest.getResourceName())) {
									AccessRequestDenialReason reason = AccessRequestDenialReason.RESOURCE_DISABLED;
									AccessRequestDeniedMsg deny = new AccessRequestDeniedMsg(access.getAccessRequest(), reason);
									access.getReplyTo().tell(deny, getSelf());
									logger.tell(LogMsg.makeAccessRequestDeniedLogMsg(access.getReplyTo(), getSelf(), access.getAccessRequest(), reason), getSelf());
								}
							}

							if (listOfAccess.isEmpty()) {
								localResource.get(manageRequest.getResourceName()).disable();

								if (!pendingDisable.containsKey(manageRequest.getResourceName())) {
									List<ManagementRequestMsg> lst = new LinkedList<ManagementRequestMsg>();
									pendingDisable.put(manageRequest.getResourceName(), lst);
								}
								pendingDisable.get(manageRequest.getResourceName()).add((ManagementRequestMsg) o);
								ManagementRequestGrantedMsg grant = new ManagementRequestGrantedMsg(manageRequest);
								replyTo.tell(grant, getSelf());
								logger.tell(LogMsg.makeResourceStatusChangedLogMsg(getSelf(), manageRequest.getResourceName(), localResource.get(manageRequest.getResourceName()).getStatus()), getSelf());
								logger.tell(LogMsg.makeManagementRequestGrantedLogMsg(replyTo, getSelf(), manageRequest), getSelf());
							} else {
								if (!pendingDisable.containsKey(manageRequest.getResourceName())) {
									List<ManagementRequestMsg> lst = new LinkedList<ManagementRequestMsg>();
									pendingDisable.put(manageRequest.getResourceName(), lst);
								}
								pendingDisable.get(manageRequest.getResourceName()).add((ManagementRequestMsg) o);
							}

					} else {
						ManagementRequestGrantedMsg grant = new ManagementRequestGrantedMsg(manageRequest);
						replyTo.tell(grant, getSelf());
						logger.tell(LogMsg.makeManagementRequestGrantedLogMsg(replyTo, getSelf(), manageRequest), getSelf());
					}
				} else {
					Resource device = localResource.get(manageRequest.getResourceName());
					if (device.getStatus() == ResourceStatus.DISABLED) {
						device.enable();
						pendingDisable.remove(manageRequest.getResourceName());
					}
					logger.tell(LogMsg.makeResourceStatusChangedLogMsg(getSelf(),manageRequest.getResourceName(), device.getStatus()), getSelf());
					logger.tell(LogMsg.makeManagementRequestGrantedLogMsg(replyTo, getSelf(), manageRequest), getSelf());
					ManagementRequestGrantedMsg grant = new ManagementRequestGrantedMsg(manageRequest);
					replyTo.tell(grant, getSelf());
				}
			}
		}

		else if (o instanceof WhoHasResourceRequestMsg) {
			ActorRef sender = getSender();
			String Resource = ((WhoHasResourceRequestMsg) o).getResourceName();
			boolean hasResource = ResourceList.contains(Resource);
			WhoHasResourceResponseMsg msg1 = new WhoHasResourceResponseMsg(Resource,hasResource,getSelf());
			sender.tell(msg1,getSelf());

		}
		else if (o instanceof WhoHasResourceResponseMsg) {

			ActorRef sender = getSender();
			String Resource = ((WhoHasResourceResponseMsg) o).getResourceName();
			boolean hasResource = ResourceList.contains(Resource);
			LogMsg msg1 = LogMsg.makeRemoteResourceDiscoveredLogMsg(getSelf(), sender, Resource);
			sender.tell(msg1,getSelf());


		}
	}
}
