package MTVS::Export::PAN;

use strict;

#-----------------------------------------------------------------------------

=head1 NAME

MTVS::Export::PAN - Production Acceptance Notification (PAN) class.

=head1 SYNOPSIS

 use MTVS::Export::PAN;
 $pan = new MTVS::Export::PAN( $PAN_name );
 $message_type = $pan->get_message_type;
 $disposition  = $pan->get_disposition;
 $time_stamp   = $pan->get_time_stamp;
 $no_of_files  = $pan->get_no_of_files;
 @files        = $pan->get_files;
 $state = "ResponseOk" if ( $pan->success );

=head1 DESCRIPTION

The PAN class provides methods for accessing the information in the PAN object
created by the new method.  The new method reads the PAN given as the input
parameter and assigns its values to a data structure.  The data structure will
differ depending on whether the PAN is a SHORTPAN or LONGPAN.

=begin text

    For SHORTPAN:

    $pan->{MESSAGE_TYPE}             - type of PAN (SHORTPAN).
    $pan->{DISPOSITION}              - disposition of ingest request.
    $pan->{TIME_STAMP}               - time when the ECS completed transfer of 
                                       file.

    For LONGPAN:

    $pan->{MESSAGE_TYPE}             - type of PAN (LONGPAN).
    $pan->{NO_OF_FILES}              - number of files in the PAN.
    $pan->{FILES}[i]{FILE_DIRECTORY} - directory location of file.
    $pan->{FILES}[i]{FILE_NAME}      - name of file.
    $pan->{FILES}[i]{DISPOSITION}    - disposition of ingest request for file.
    $pan->{FILES}[i]{TIME_STAMP}     - time when the ECS completed transfer of 
                                       file.

=end text

where i is between 0 and $pan->{NO_OF_FILES}-1.

The data structure is given for reference.  All accesses to the data structure 
should be made through the get_message_type, get_disposition, get_time_stamp, 
get_no_of_files, and get_files methods.  The get_disposition and get_time_stamp 
methods will fail if the MESSAGE_TYPE is LONGPAN.  The get_no_of_files and 
get_files methods will fail if the MESSAGE_TYPE is SHORTPAN.

=over 4

=item $pan = new MTVS::Export::PAN( $PAN_name )

Constructs a PAN object and initializes it with the contents of the PAN file.
Returns PAN object if successful.  Returns undef if PAN file cant be opened 
or contains garbage.  $PAN_name is the full path name of a PAN file.  
Returns reference to a PAN object.

=back

=cut

#-----------------------------------------------------------------------------
#-----------------------------------------------------------------------------
sub new {

    # Construct object
    my $class = shift;
    my $self = { };
    bless( $self, $class );

    # Initialize object

    # Get name of PAN
    my $name = shift;
    my $junk;

    # Open PAN
    open( PAN, $name ) or return 0;

    # Parse PAN
    while ( <PAN> ) {

	# remove semicolon
        ($_, $junk) = split ';';
        $_ =~ s/^\s+//;   # Remove leading blanks
        $_ =~ s/\s+$//;   # Remove trailing blanks

        # Extract parameter and value from line
	my ($parameter,$value) = /(\w+)\s*=\s*\"?([^\"]+)/x;
	if (!defined $parameter) {
            # don't return error for TIME_STAMP field (EDC may have empty field for this field)
            if (/TIME_STAMP/)
            {
                warn "IFailed to parse '$_' (param = value) in $name";
                $parameter = "TIME_STAMP";
                $value = "NA";
            }
            else
            {
                warn "EFailed to parse '$_' (param = value) in $name";
                return undef;   
            }
	}

	# Assign parameter and value to object hash reference
	if ( $parameter eq "FILE_DIRECTORY" and 
	     $self->{MESSAGE_TYPE} eq "LONGPAN" ) 
	{
	    push @{ $self->{FILES} }, { $parameter => $value };
	} elsif ( ( $parameter eq "FILE_NAME" or 
	            $parameter eq "DISPOSITION" or 
	            $parameter eq "TIME_STAMP" ) and 
	          $self->{MESSAGE_TYPE} eq "LONGPAN" ) 
	{
            # in case that the FILE_DIRECTORY is not found first
            if ( $#{$self->{FILES}} < 0 ) {
                warn "EFailed to parse '$_', no FILE_DIRECTORY found";
                return undef;   
            }
	    $self->{FILES}[$#{ $self->{FILES} }]{$parameter}= $value;
	} else {
	    $self->{$parameter} = $value;
	} #endif
# print "parameter=$parameter,value=$value,\n";

    } #endwhile

    # Return object
    return $self;

} #endsub


#-----------------------------------------------------------------------------

=over 4

=item $message_type = $pan->get_message_type()

Returns $self->{MESSAGE_TYPE}, the MESSAGE_TYPE of the PAN.

=back

=cut

#-----------------------------------------------------------------------------
sub get_message_type {
    my $self = shift;
    return $self->{MESSAGE_TYPE};
} #endsub


#-----------------------------------------------------------------------------

=over 4

=item $disposition = $pan->get_disposition()

Returns $self->{DISPOSITION}, the DISPOSITION of the PAN.
This method should only be used on short form PANs.  
Use the get_files method to get the DISPOSITION of each file in long form PANs.

=back

=cut

#-----------------------------------------------------------------------------
sub get_disposition {
    my $self = shift;
    if ($self->{MESSAGE_TYPE} eq "LONGPAN")
    {
       print "WARNING: Invalid method for LONGPAN MESSAGE_TYPE";
       return 0;
    }
    return $self->{DISPOSITION};
} #endsub


#-----------------------------------------------------------------------------

=over 4

=item $time_stamp = $pan->get_time_stamp()

Returns $self->{TIME_STAMP}, the TIME_STAMP of the PAN, when the ECS completed 
transfer of file.  This method should only be used on short form PANs.  
Use the get_files method to get the TIME_STAMP of each file in long form PANs.

=back

=cut

#-----------------------------------------------------------------------------
sub get_time_stamp {
    my $self = shift;
    if ($self->{MESSAGE_TYPE} eq "LONGPAN")
    {
       print "WARNING: Invalid method for LONGPAN MESSAGE_TYPE";
       return 0;
    }
    return $self->{TIME_STAMP};
} #endsub

#-----------------------------------------------------------------------------

=over 4

=item $no_of_files = $pan->get_no_of_files()

Returns $self->{NO_OF_FILES} - number of files in the PAN.
This method should only be used on long form PANs.

=back

=cut

#-----------------------------------------------------------------------------
sub get_no_of_files {
    my $self = shift;
    if ($self->{MESSAGE_TYPE} eq "SHORTPAN")
    {
       print "WARNING: Invalid method for SHORTPAN MESSAGE_TYPE"; 
       return 0;
    }
    return $self->{NO_OF_FILES};
} #endsub


#-----------------------------------------------------------------------------

=over 4

=item @files = $pan->get_files()

Returns @{ $self->{FILES} },
an array of hashes with the FILE_DIRECTORY, FILE_NAME, DISPOSITION, and 
TIME_STAMP for each file. This method should only be used on long form PANs.
The array of hashes has the following format:

=begin text

        @files = 
        [
            {
                FILE_DIRECTORY => value, 
                FILE_NAME      => value, 
                DISPOSITION    => value, 
                TIME_STAMP     => value, 
            },
            {
                FILE_DIRECTORY => value, 
                FILE_NAME      => value, 
                DISPOSITION    => value, 
                TIME_STAMP     => value, 
            },
            etc.
        ];

=end text

=back

=cut

#-----------------------------------------------------------------------------
sub get_files {
    my $self = shift;
    if ($self->{MESSAGE_TYPE} eq "SHORTPAN")
    {
       print "WARNING: Invalid method for SHORTPAN MESSAGE_TYPE";
       return 0;
    }
    return @{ $self->{FILES} };
} #endsub


#-----------------------------------------------------------------------------

=over 4

=item $pan->success()

Returns 1 if the PAN is successful; 0 otherwise.  PAN is successful if
$self->{DISPOSITION} eq "SUCCESSFUL" (for $self->{MESSAGE_TYPE} eq "SHORTPAN")
or if each $self->{FILES}[]{DISPOSITION} eq "SUCCESSFUL" 
(for $self->{MESSAGE_TYPE} eq "LONGPAN").

=back

=cut

#-----------------------------------------------------------------------------
sub success {

    my $self = shift;

    if ( $self->{MESSAGE_TYPE} eq "SHORTPAN" ) {
	if ( $self->{DISPOSITION} ne "SUCCESSFUL" ) {
	    return 0;
	} #endif
        return 1;
    } elsif ( $self->{MESSAGE_TYPE} eq "LONGPAN" ) {
        for ( @ { $self->{FILES} } ) {
	    if ( $_->{DISPOSITION} ne "SUCCESSFUL" ) {
	        return 0;
	    } #endif
	} #endfor
	return 1;
    } else {
        print "WARNING: Invalid MESSAGE_TYPE:$self->{MESSAGE_TYPE}.\n";
        return 0;
    } #endif

} #endsub

#-----------------------------------------------------------------------------

=head1 AUTHOR

Karen Horrocks, September 2000

=head1 ACKNOWLEDGMENTS

This software is developed by the MODAPS Team for the National Aeronautics and 
Space Administration, Goddard Space Flight Center, under contract NAS5-32373.

=cut

#-----------------------------------------------------------------------------
1;

